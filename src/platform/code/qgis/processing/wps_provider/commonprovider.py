from qgis.core import QgsProcessingProvider

from .processes.pinresult import PinResult
from .processes.savetodatabase import SaveToDatabase
from .processes.createtable import CreateTable
from .processes.mapstate import ImportMapState, DeleteMapState
from .processes.publishfile import PublishFile


class CommonProvider(QgsProcessingProvider):
    """
    This provider acts as a container for a set of general common processing algorithms
    """

    def __init__(self):
        super().__init__()

        # Obtain list of algorithms supplied by this provider
        self.algs = self.getAlgs()

    def id(self):
        """
        Unique identifier for the provider
        """
        return "common"

    def name(self):
        """
        Returns provider name
        """
        return f"process.group.common"

    def getAlgs(self):
        """
        Define list of algorithms
        """
        algorithms = [
            PinResult(),
            # CreateTable(),
            # SaveToDatabase(),
            ImportMapState(),
            DeleteMapState(),
            PublishFile(),
        ]

        return algorithms

    def loadAlgorithms(self):
        """
        Loads all algorithms belonging to this provider
        """
        for a in self.algs:
            self.addAlgorithm(a)
