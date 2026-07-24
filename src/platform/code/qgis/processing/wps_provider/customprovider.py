#%@
if (!feature.MV_Processes) return [];
return [{ fileName: fileName, basePath: basePath, context: context }];
%#
import os
import json

from qgis.core import QgsProcessingProvider
from .processes.processwrapper import RasterWrapper, VectorWrapper


class CustomProvider(QgsProcessingProvider):
    """
    This provider acts as a container for a set of processing algorithms defined by passed 'identifier'
    """

    def __init__(self, identifier):
        super().__init__()

        # Set id
        self.identifier = identifier

        # Obtain provider configuration from id
        provider_path = os.path.dirname(os.path.abspath(__file__))
        providers_conf = f"{provider_path}/resources/providers.json"

        with open(providers_conf, encoding="UTF-8") as f:
            self.provider_config = json.load(f)[identifier]

        # Obtain list of algorithms supplied by this provider
        self.algs = self.getAlgs()

    def id(self):
        """
        Unique identifier for the provider
        """
        return self.identifier

    def name(self):
        """
        Returns provider name
        """
        return f"process.group.{self.id()}"

    def getAlgs(self):
        """
        Define list of algorithms
        """
        algorithms = []

        # Get provider algorithms from configuration
        class_name = globals()[self.provider_config["class"]]
        for algorithm in self.provider_config["algorithms"]:
            algorithms.append(class_name(algorithm))

        return algorithms

    def loadAlgorithms(self):
        """
        Loads all algorithms belonging to this provider
        """
        for a in self.algs:
            self.addAlgorithm(a)
