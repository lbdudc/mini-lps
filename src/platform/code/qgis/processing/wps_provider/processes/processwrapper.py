#%@
if (!feature.MV_Processes) return [];
return [{ fileName: fileName, basePath: basePath, context: context }];
%#
import traceback
import json
import os
import re

from qgis import processing

from qgis.core import (
    QgsWkbTypes,
    QgsApplication,
    QgsProcessingException,
    QgsProcessingAlgorithm,
    QgsProcessingOutputString,
    QgsProcessingParameterDefinition,
    QgsProcessingDestinationParameter,
    QgsProcessingParameterFolderDestination,
)

provider_path = os.path.split(os.path.dirname(__file__))[0]


class ProcessWrapper(QgsProcessingAlgorithm):
    """
    Processing algorithm that wraps around a QGIS process based on 'process_name'
    """

    INPUT = "INPUT"
    OUTPUT = "OUTPUT"
    SERVICE = "SERVICE"

    def __init__(self, process_name):
        super().__init__()

        # Set native process from algorithm identifier
        self.process_name = process_name
        self.native_process = QgsApplication.processingRegistry().algorithmById(
            self.process_name
        )

        # Obtain params configuration
        params_path = f"{provider_path}/resources/params.json"
        with open(params_path, encoding="UTF-8") as f:
            self.params_config = json.load(f)

    def createInstance(self):
        """
        Return a new instance of the algorithm
        """
        return self.__class__(self.process_name)

    def name(self):
        """
        Returns the unique algorithm name
        """
        return self.native_process.name()

    def displayName(self):
        """
        Returns a reference to the translated algorithm name
        """
        return f"process.{self.native_process.name()}.title"

    def group(self):
        """
        Returns a reference to the translated name of the group
        this algorithm belongs to
        """
        return f"process.group.{self.native_process.groupId()}"

    def groupId(self):
        """
        Returns the unique ID of the group this algorithm belongs
        to
        """
        return self.native_process.groupId()

    def shortHelpString(self):
        """
        Returns a reference to a short helper string for the algorithm
        """
        return f"process.{self.native_process.name()}.description"

    def initAlgorithm(self, config=None):
        """
        Initializes the algorithm, including inputs and outputs definition
        """
        # Inherit parameters from native process
        for param in self.native_process.parameterDefinitions():
            clone = param.clone()
            clone.setDescription(f"param.{param.name().lower()}.title")
            clone.setHelp(f"param.{param.name().lower()}.description")

            # Make destination params mandatory to deal with check_valid() error when empty
            if isinstance(clone, QgsProcessingDestinationParameter):
                # Limit accepted destinations to files only
                if isinstance(clone, QgsProcessingParameterFolderDestination):
                    continue

                clone.setFlags(
                    clone.flags() & ~QgsProcessingParameterDefinition.FlagOptional
                )
                clone.setDefaultValue(f"{self.name().lower()}_{param.name().lower()}")

            self.addParameter(clone)

        # Custom process outputs
        self.addOutput(QgsProcessingOutputString(self.SERVICE, "param.service.title"))

    def init_params(self, parameters, context, feedback, params):
        """
        Initialize process inputs
        """
        # Get native algorithm input params
        for param in self.native_process.parameterDefinitions():
            if param.name() in params:
                continue

            try:
                # Directly obtain parameter evaluation for basic types
                config_entry = self.params_config.get(param.type())
                if not config_entry:
                    # Extract basic type from param.asPythonString()
                    param_regex = re.search(
                        r"type=([a-zA-Z.]+)", param.asPythonString()
                    )
                    param_type = param_regex.group(1).split(".")[1].lower()
                    config_entry = self.params_config.get(param_type)

                method = getattr(self, config_entry["method"])

            except Exception as e:
                feedback.pushInfo(f"Error evaluating param '{param.name()}': {e}")
                continue

            if method and (param_eval := method(parameters, param.name(), context)):
                params[param.name()] = param_eval

            if feedback.isCanceled():
                return {}

        return params

    def init_output(self, feedback, process_result, output):
        """
        Set process outputs
        """
        # Get native algorithm input params
        for param in self.native_process.outputDefinitions():
            # Exclude output params not included in the native process result
            if param.name() in process_result:
                output[param.name()] = process_result[param.name()]

            if feedback.isCanceled():
                return {}

        return output


class VectorWrapper(ProcessWrapper):
    """
    Processing algorithm that wraps around a QGIS vector based process based on 'process_name'
    """

    def processAlgorithm(self, parameters, context, feedback):
        """
        Runs the algorithm using the specified ``parameters``
        """
        try:
            # Retrieve vector layer from QgsProject
            source = self.parameterAsSource(parameters, self.INPUT, context)

            vector_layer = context.project().mapLayersByName(source.sourceName())[0]
            if not vector_layer.isValid():
                raise QgsProcessingException(
                    "Could not create a vector layer from source."
                )

            if feedback.isCanceled():
                return {}

            # Create output sink
            output_param = self.native_process.parameterDefinition(self.OUTPUT)

            # Transform ProcessingSourceType to WkbType
            GEOMETRY_MAP = {
                0: QgsWkbTypes.Type.Point,
                1: QgsWkbTypes.Type.MultiPoint,
                2: QgsWkbTypes.Type.LineString,
                3: QgsWkbTypes.Type.MultiLineString,
            }
            geom_type = GEOMETRY_MAP.get(
                output_param.dataType(), QgsWkbTypes.Type.Polygon
            )

            (output_sink, output_dest) = self.parameterAsSink(
                parameters,
                self.OUTPUT,
                context,
                vector_layer.fields(),
                geom_type,
                vector_layer.sourceCrs(),
            )

            if feedback.isCanceled():
                return {}

            # Parse process input parameters
            custom_params = {self.INPUT: vector_layer, self.OUTPUT: output_dest}
            process_params = self.init_params(
                parameters, context, feedback, custom_params
            )

            # Perform the native process
            process_result = processing.run(
                self.process_name, process_params, context=context, feedback=feedback
            )

            if feedback.isCanceled():
                return {}

            # Parse process output parameters
            custom_output = {self.SERVICE: "WFS"}
            return self.init_output(feedback, process_result, custom_output)

        except Exception as e:
            traceback.print_exc()

            raise QgsProcessingException(f"Error running task. Details: {str(e)}")


class RasterWrapper(ProcessWrapper):
    """
    Processing algorithm that wraps around a QGIS raster based process based on 'process_name'
    """

    def processAlgorithm(self, parameters, context, feedback):
        """
        Runs the algorithm using the specified ``parameters``
        """
        try:
            # Parse process input parameters
            custom_params = {self.OUTPUT: parameters[self.OUTPUT]}
            process_params = self.init_params(
                parameters, context, feedback, custom_params
            )

            if feedback.isCanceled():
                return {}

            # Perform the native process
            process_result = processing.run(
                self.process_name, process_params, context=context, feedback=feedback
            )

            print(process_result)

            if feedback.isCanceled():
                return {}

            # Parse process output parameters
            custom_output = {self.SERVICE: "WCS"}
            return self.init_output(feedback, process_result, custom_output)

        except Exception as e:
            traceback.print_exc()

            raise QgsProcessingException(f"Error running task. Details: {str(e)}")
