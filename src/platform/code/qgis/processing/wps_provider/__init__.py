#%@
if (!feature.MV_Processes) return [];
return [{ fileName: fileName, basePath: basePath, context: context }];
%#
import os
import json

from .customprovider import CustomProvider
from .commonprovider import CommonProvider


def WPSClassFactory(iface):
    """
    Factory entrypoint for processing providers, registers providers defined in the configuration
    """
    provider_path = os.path.dirname(os.path.abspath(__file__))
    providers_conf = os.path.join(provider_path, "resources", "providers.json")

    # Register common provider
    iface.registerProvider(CommonProvider())

    # Register custom providers
    with open(providers_conf, encoding="UTF-8") as f:
        providers = json.load(f)
        for group in providers:
            iface.registerProvider(CustomProvider(group))
