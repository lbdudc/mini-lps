#% if (feature.MV_Processes) { %#
import traceback
import shutil
import uuid
import json
import time
import os
import zipfile

from ..utils.layerfactory import LayerFactory
from ..utils.filterfactory import FilterFactory
from qgis.core import (
    QgsProject,
    QgsProcessingException,
    QgsProcessingAlgorithm,
    QgsProcessingOutputString,
    QgsProcessingParameterNumber,
    QgsCoordinateReferenceSystem,
    QgsProcessingParameterString,
)

provider_path = os.path.split(os.path.dirname(__file__))[0]


class ImportMapState(QgsProcessingAlgorithm):
    """
    Processing algorithm that creates a qgis project from a map state.
    """

    MAP_STATE = "MAP_STATE"
    MAP_ID = "MAP_ID"
    REF_ID = "REF_ID"
    MESSAGE = "MESSAGE"

    def __init__(self):
        super().__init__()

    def createInstance(self, config={}):
        """
        Return a new instance of the algorithm
        """
        return self.__class__()

    def name(self):
        """
        Returns the unique algorithm name.
        """
        return "importmapstate"

    def displayName(self):
        """
        Returns the algorithm display name.
        """
        return "Import map state"

    def initAlgorithm(self, config=None):
        """
        Here we define the inputs and outputs of the algorithm.
        """
        # process inputs
        self.addParameter(QgsProcessingParameterString(self.MAP_STATE, "Map state"))

        self.addParameter(
            QgsProcessingParameterNumber(self.MAP_ID, "Custom map id", optional=True)
        )

        # process outputs
        self.addOutput(
            QgsProcessingOutputString(self.REF_ID, "Reference to created qgis map")
        )

        self.addOutput(QgsProcessingOutputString(self.MESSAGE, "Output message"))

    def processAlgorithm(self, parameters, context, feedback):
        """
        Here is where the processing itself takes place.
        """
        temp_files = []
        unsupported_types = ("TILE", "TIMESERIESWMS", "TIMESERIESWCS")

        try:
            # Create a new QGIS project
            project = QgsProject.instance()
            project.clear()

            # Set coordinate system
            crs = QgsCoordinateReferenceSystem("EPSG:4326")
            project.setCrs(crs)

            id_ref = self.parameterAsInt(parameters, self.MAP_ID, context)
            if not id_ref:
                id_ref = str(uuid.uuid4())

            # Import map state into new QGIS project
            map_state = json.loads(
                self.parameterAsString(parameters, self.MAP_STATE, context)
            )

            # Create layers from map state
            layer_factory = LayerFactory(feedback)
            filter_factory = FilterFactory(feedback)
            if "layers" in map_state:
                for layer_state in map_state["layers"]:
                    layer_options = layer_state["options"]

                    if layer_options["type"] in unsupported_types:
                        feedback.pushInfo(
                            f"Layer type {layer_options['type']} not supported"
                        )
                        continue

                    # Create layer
                    layer_options["map"] = id_ref
                    qgs_layer = layer_factory.init_layer(layer_options)
                    if not qgs_layer:
                        feedback.pushInfo(
                            f"Could not create layer {layer_options['id']}"
                        )
                        continue

                    if "filters" in layer_options and layer_options["filters"]:
                        qgs_layer = filter_factory.apply_filter(
                            qgs_layer, layer_options["filters"]
                        )

                    if qgs_layer.isValid():
                        project.addMapLayer(qgs_layer)

            # Write project as .qgs to /projects/ (layers saved there too, so paths persist)
            project_path = f"/projects/{id_ref}.qgs"
            feedback.pushInfo(f"Writing project to {project_path}")
            project_success = project.write(project_path)
            file_exists = os.path.exists(project_path)
            feedback.pushInfo(f"project.write() returned: {project_success}, file exists: {file_exists}")

            if not file_exists:
                raise QgsProcessingException(f"Project file not created: write={project_success}, exists={file_exists}")

            return {
                self.REF_ID: id_ref,
                self.MESSAGE: "Success importing map state into a QGIS project.",
            }

        except Exception as e:
            traceback.print_exc()

            raise QgsProcessingException(
                f"Error importing map state. Details: {str(e)}"
            )

        finally:
            for temp_file in temp_files:
                os.remove(temp_file)


class DeleteMapState(QgsProcessingAlgorithm):
    """
    Processing algorithm that deletes a stored qgis project.
    """

    MAP_ID = "MAP_ID"
    MESSAGE = "MESSAGE"

    def __init__(self):
        super().__init__()

    def createInstance(self, config={}):
        """
        Return a new instance of the algorithm
        """
        return self.__class__()

    def name(self):
        """
        Returns the unique algorithm name.
        """
        return "deletemapstate"

    def displayName(self):
        """
        Returns the algorithm display name.
        """
        return "Delete map state"

    def initAlgorithm(self, config=None):
        """
        Here we define the inputs and outputs of the algorithm.
        """
        # process inputs
        self.addParameter(QgsProcessingParameterString(self.MAP_ID, "Map id"))

        # process outputs
        self.addOutput(QgsProcessingOutputString(self.MESSAGE, "Output message"))

    def processAlgorithm(self, parameters, context, feedback):
        """
        Here is where the processing itself takes place.
        """
        try:
            map_id = self.parameterAsString(parameters, self.MAP_ID, context)
            project_path = f"/projects/{map_id}.qgs"
            layer_dir = f"/projects/{map_id}"

            if os.path.exists(project_path):
                os.remove(project_path)
            if os.path.exists(layer_dir):
                shutil.rmtree(layer_dir)

            if not os.path.exists(project_path) and not os.path.exists(layer_dir):
                pass  # successfully deleted
            else:
                raise QgsProcessingException(f"Could not delete project files for: {map_id}")

            return {self.MESSAGE: "Success deleting stored map state."}

        except Exception as e:
            traceback.print_exc()

            raise QgsProcessingException(f"Error deleting map state. Details: {str(e)}")

#% } %#
