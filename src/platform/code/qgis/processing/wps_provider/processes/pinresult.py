import traceback
import json

from pyqgiswps.executors import logstore
from qgis.core import (
    QgsProcessingException,
    QgsProcessingAlgorithm,
    QgsProcessingOutputString,
    QgsProcessingParameterString,
)


class PinResult(QgsProcessingAlgorithm):
    """
    Processing algorithm that makes task result permanent in memory.
    """

    TASK_ID = "TASK_ID"
    MESSAGE = "MESSAGE"

    def __init__(self):
        super().__init__()

    def createInstance(self):
        """
        Return a new instance of the algorithm
        """
        return self.__class__()

    def name(self):
        """
        Returns the unique algorithm name
        """
        return "pinresult"

    def displayName(self):
        """
        Returns the algorithm display name.
        """
        return "Pin result by id"

    def initAlgorithm(self, config=None):
        """
        Initializes the algorithm, including inputs and outputs definition
        """
        self.addParameter(QgsProcessingParameterString(self.TASK_ID, "Task ID"))

        self.addOutput(QgsProcessingOutputString(self.MESSAGE, "Output message"))

    def processAlgorithm(self, parameters, context, feedback):
        """
        Here is where the processing itself takes place.
        """
        try:
            task_id = self.parameterAsString(parameters, self.TASK_ID, context)

            logstore_inst = logstore.logstore
            task_status = logstore_inst.get_status(task_id)

            if task_status is not None:
                record = task_status

                if logstore.STATUS[record["status"]] != logstore.STATUS.DONE_STATUS:
                    raise ValueError("Invalid status for %s" % task_id)
                else:
                    record["pinned"] = True
                    record["expire_at"] = None

                # update the record
                logstore_inst._db.hset(
                    logstore_inst._hstatus, task_id, json.dumps(record)
                )

                return {self.MESSAGE: "Success making jobs results permanent."}
            else:
                raise FileNotFoundError("No status for %s" % task_id)

        except Exception as e:
            traceback.print_exc()

            raise QgsProcessingException(
                f"Error saving results from task {task_id}. Details: {str(e)}"
            )
