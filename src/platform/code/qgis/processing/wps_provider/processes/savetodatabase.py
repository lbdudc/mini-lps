#% if (feature.MV_Processes) { %#
import traceback
import json

from ..utils.utils import (
    get_vector_layer,
    get_results_directory,
    get_postgres_connection,
)
from pyqgiswps.executors import logstore
from qgis.core import (
    QgsVectorLayer,
    QgsProcessingException,
    QgsProcessingAlgorithm,
    QgsProcessingOutputString,
    QgsProcessingParameterString,
)


class SaveToDatabase(QgsProcessingAlgorithm):
    """
    Processing algorithm that saves a task result to database into t_qgis_process_result table.

    NOTE: This will only work if the task result is a vector layer produced by a vector processing algorithm.
    """

    TASK_ID = "TASK_ID"
    OUTPUT = "OUTPUT"
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
        return "savetodb"

    def displayName(self):
        """
        Returns the algorithm display name.
        """
        return "Save to database"

    def initAlgorithm(self, config=None):
        """
        Here we define the inputs and outputs of the algorithm.
        """
        # process inputs
        self.addParameter(QgsProcessingParameterString(self.TASK_ID, "Task ID"))

        # process outputs
        self.addOutput(QgsProcessingOutputString(self.MESSAGE, "Output message"))

    def processAlgorithm(self, parameters, context, feedback):
        """
        Here is where the processing itself takes place.
        """
        task_id = self.parameterAsString(parameters, self.TASK_ID, context)

        try:
            # Retrieve task results
            task_results = logstore.logstore.get_results(task_id)
            if task_results is None:
                raise FileNotFoundError(
                    f"Could not retrieve results from task {task_id}."
                )
            decoded_result = json.loads(task_results.decode("utf-8"))

            # Create vector layer
            results_dir = get_results_directory(decoded_result)
            layer = get_vector_layer(results_dir)

            # Connect to Postgres
            connection = get_postgres_connection()
            cursor = connection.cursor()

            # Prepare insert query
            fields = ["task", "params"]
            insert_query = f"""
                INSERT INTO public.{"t_qgis_process_result"} (geom, {', '.join(fields)})
                VALUES (%s, {', '.join(['%s'] * len(fields))});
            """

            # Insert each feature
            for feature in layer.getFeatures():
                geom_wkt = feature.geometry().asWkt()
                task_id = self.parameterAsString(parameters, self.TASK_ID, context)
                properties_json = json.dumps(feature.attributes())
                cursor.execute(insert_query, [geom_wkt, task_id, properties_json])

            # Commit the changes and close the connection
            connection.commit()
            connection.close()

            return {self.MESSAGE: "Success saving task result."}

        except Exception as e:
            traceback.print_exc()

            raise QgsProcessingException(
                f"Error saving results from task { self.parameterAsString(parameters, self.TASK_ID, context) }. Details: { str(e) }"
            )

#% } %#
