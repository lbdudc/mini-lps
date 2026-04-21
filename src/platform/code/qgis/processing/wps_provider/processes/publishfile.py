import os
import json
import traceback

from ..utils.layerfactory import LayerFactory
from osgeo import ogr
from qgis.core import (
    QgsProject,
    QgsVectorLayer,
    QgsRasterLayer,
    QgsProcessingContext,
    QgsProcessingAlgorithm,
    QgsProcessingOutputString,
    QgsProcessingParameterString,
)


class PublishFile(QgsProcessingAlgorithm):
    """
    Processing algorithm that loads file directly from a filesystem path.

    NOTE:
      - The input path must refer to a file that is accessible inside the py-qgis-wps/py-qgis-server container environment.
      - This algorithm does NOT upload or copy the file; it simply references it.
    """

    FILE_OPTIONS = "FILE_OPTIONS"
    OUTPUT = "OUTPUT"
    JOB_ID = "JOB_ID"
    SERVICE = "SERVICE"

    def createInstance(self, config={}):
        """
        Return a new instance of the algorithm
        """
        return self.__class__()

    def name(self):
        """
        Returns the unique algorithm name.
        """
        return "publishfile"

    def displayName(self):
        """
        Returns the algorithm display name.
        """
        return "Publish File"

    def initAlgorithm(self, config=None):
        """
        Here we define the inputs and outputs of the algorithm.
        """
        self.addParameter(
            QgsProcessingParameterString(self.FILE_OPTIONS, "File description")
        )

        self.addOutput(QgsProcessingOutputString(self.OUTPUT, "Layer access URL"))

        self.addOutput(QgsProcessingOutputString(self.JOB_ID, "Job identifier"))

        self.addOutput(
            QgsProcessingOutputString(self.SERVICE, "Service type (WFS or WCS)")
        )

    def processAlgorithm(self, parameters, context, feedback):
        """
        Here is where the processing itself takes place.
        """
        try:
            file_options = json.loads(
                self.parameterAsString(parameters, self.FILE_OPTIONS, context)
            )

            # Create layer
            layer_factory = LayerFactory(feedback)
            qgs_layer = layer_factory.init_layer(file_options)
            if not qgs_layer:
                feedback.pushInfo(f"Could not create layer {file_options['id']}")

            # Add layer to temporary store
            added_layer = context.temporaryLayerStore().addMapLayer(qgs_layer)
            if added_layer is None:
                raise Exception("Layer failed to register into the project")
            else:
                feedback.pushInfo(f"Layer added: {qgs_layer.name()}")

            details = QgsProcessingContext.LayerDetails()
            details.name = qgs_layer.name()
            details.outputName = qgs_layer.name()

            # Persist layer once processing finishes
            context.addLayerToLoadOnCompletion(qgs_layer.id(), details)

            # Determine server access
            base_url = os.environ.get("QGSWPS_SERVER_OWS_SERVICE_URL", "")

            # Determine access service type
            service_type = None
            if isinstance(qgs_layer, QgsVectorLayer):
                service_type = "WFS"
            elif isinstance(qgs_layer, QgsRasterLayer):
                service_type = "WCS"

            # Determine output url
            output_url = f"{base_url}?MAP={context.workdir}/common_publishfile.qgis&service={service_type}&request=GetCapabilities&layers={file_options['id']}"

            return {
                self.OUTPUT: output_url,
                self.JOB_ID: os.path.basename(context.workdir.rstrip("/")),
                self.SERVICE: service_type,
            }

        except Exception as e:
            traceback.print_exc()
            raise Exception(f"Error in process: {str(e)}")
