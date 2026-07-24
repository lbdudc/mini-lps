#%@
if (!feature.MV_Processes) return [];
return [{ fileName: fileName, basePath: basePath, context: context }];
%#
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


class CreateTable(QgsProcessingAlgorithm):
    """
    Processing algorithm that creates table in database form a task result.

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
        return "createtable"

    def displayName(self):
        """
        Returns the algorithm display name.
        """
        return "Create table"

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

            MAP_QGIS_TO_PG_TYPE = {
                "Integer": "INTEGER",
                "Integer64": "BIGINT",
                "UInt32": "BIGINT",
                "UInt64": "NUMERIC",
                "Real": "DOUBLE PRECISION",
                "Double": "DOUBLE PRECISION",
                "String": "TEXT",
                "Boolean": "BOOLEAN",
                "Date": "DATE",
                "DateTime": "TIMESTAMP",
                "Time": "TIME",
                "ByteArray": "BYTEA",
            }

            columns = ["geom geometry"]
            for field in layer.fields():
                pg_type = MAP_QGIS_TO_PG_TYPE.get(field.typeName(), "TEXT")
                columns.append(f'"{field.name()}" {pg_type}')

            # Build CREATE TABLE query
            table_name = f"t_qgis_{task_id.replace('-', '_')}"
            create_query = f"CREATE TABLE public.{table_name} ({', '.join(columns)});"
            cursor.execute(create_query)

            # Prepare insert query
            field_names = [field.name() for field in layer.fields()]
            col_names = ["geom"] + [f'"{name}"' for name in field_names]
            insert_query = f"""
                INSERT INTO public.{table_name} ({', '.join(col_names)})
                VALUES ({', '.join(['%s'] * len(col_names))});
            """

            # Insert each feature
            for feat in layer.getFeatures():
                geom_wkt = feat.geometry().asWkt()
                attr_values = [feat[name] for name in field_names]
                cursor.execute(insert_query, [geom_wkt] + attr_values)

            # Commit the changes and close the connection
            connection.commit()
            connection.close()

            return {self.MESSAGE: "Success saving task result."}

        except Exception as e:
            traceback.print_exc()

            raise QgsProcessingException(
                f"Error saving results from task { self.parameterAsString(parameters, self.TASK_ID, context) }. Details: { str(e) }"
            )
