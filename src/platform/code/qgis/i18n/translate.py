import argparse
import json
import sys
import os
import re

from qgis.core import QgsApplication
from qgis.PyQt.QtCore import QTranslator, QCoreApplication

# Mapping of OS to theis respective translation file locations
TRANSLATION_PATHS = {
    "win": "C:/Program Files/QGIS 3.40.7/apps/qt5/translations/qt_{lang}.qm",
    "linux": "/usr/share/qgis/i18n/qgis_{lang}.qm",
}


def tr_clean(context, text):
    """
    Translate the text and replace multiple newlines with a single space.

    :param text: String to translate.
    :param context: Translation context.
    """
    translated_text = QCoreApplication.translate(context, text)
    return re.sub(r"\s+", " ", translated_text).strip()


def get_qm_file(language_code):
    """
    Returns the correct .qm file path based on the detected OS.

    :param language_code: language code (e.g., 'es').
    """
    platform_key = next(
        (key for key in TRANSLATION_PATHS if sys.platform.startswith(key)), None
    )

    return (
        TRANSLATION_PATHS.get(platform_key, "").format(lang=language_code)
        if platform_key
        else None
    )


def load_translations(qgis_application, qm_file):
    """
    Retrieve details about a QGIS processing algorithm.

    :param qgis_application: Instance of QGIS application.
    :param qm_file: The name of the process (e.g., 'native:buffer').
    """
    # Create a QTranslator object and load translations for the specified language
    translator = QTranslator()

    if not translator.load(qm_file):
        print(f"Failed to load translation file: {qm_file}")
        return None

    qgis_application.installTranslator(translator)


def get_algorithm_info(qgis_application, process_name, translation_context):
    """
    Retrieve details about a QGIS processing algorithm.

    :param qgis_application: Instance of QGIS application.
    :param process_name: The name of the process (e.g., 'native:buffer').
    :param translation_context: Translations context.
    """
    # Get algorithm
    alg = qgis_application.processingRegistry().algorithmById(process_name)

    if not alg:
        return f"Algorithm '{process_name}' not found."

    # Extract algorithm details
    process_info = {
        "process": {
            alg.id().split(":")[-1]: {
                "title": alg.displayName(),
                "description": tr_clean(translation_context, alg.shortHelpString()),
            }
        }
    }

    # Extract input/output parameters details
    param_info = {"param": {}}

    for input_param in alg.parameterDefinitions():
        param_info["param"][input_param.name().lower()] = {
            "title": tr_clean(translation_context, input_param.description()),
            "description": tr_clean(translation_context, input_param.help()),
        }

    for output_param in alg.outputDefinitions():
        param_info["param"][output_param.name().lower()] = {
            "title": tr_clean(
                translation_context, output_param.description()
            ),  # output_param.name().lower()
            "description": "",
        }

    # Sort parameters alphabetically by key
    param_info["param"] = dict(sorted(param_info["param"].items()))

    return {**process_info, **param_info}


if __name__ == "__main__":
    # Define argument parser
    parser = argparse.ArgumentParser(
        description="Extract translation for a QGIS native process."
    )
    parser.add_argument("process_name", help="The process name")
    parser.add_argument(
        "context_name",
        help="The translation context (default to 'QObject')",
        nargs="?",
        default="QObject",
    )

    # Parse the arguments
    args = parser.parse_args()

    os.environ["QT_QPA_PLATFORM"] = "offscreen"
    os.environ["XDG_SESSION_TYPE"] = "xcb"

    # Initialize QGIS path
    qgisPrefixPath = os.environ.get("QGIS_PREFIX_PATH", "/usr/")
    sys.path.append(os.path.join(qgisPrefixPath, "share/qgis/python/plugins/"))

    # Initialize application
    qgis_application = QgsApplication([], False)
    qgis_application.setPrefixPath(qgisPrefixPath, True)
    qgis_application.initQgis()

    # Initialize Processing framework
    from processing.core.Processing import Processing

    Processing.initialize()

    for lang in ["en_US", "es", "gl"]:
        print(f"Translation ({lang.upper()}):\n")

        qm_file = get_qm_file(lang)
        load_translations(qgis_application, qm_file)
        qgis_application.setTranslation(lang)

        # Get algorithm info by name
        process_name = args.process_name
        translation_context = args.context_name
        algorithm_info = get_algorithm_info(
            qgis_application, process_name, translation_context
        )

        print(json.dumps(algorithm_info, indent=2, ensure_ascii=False), "\n")

    qgis_application.exitQgis()
