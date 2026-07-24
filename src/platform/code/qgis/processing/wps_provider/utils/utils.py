#%@
if (!feature.MV_Processes) return [];
return [{ fileName: fileName, basePath: basePath, context: context }];
%#
import os
import psycopg2
import json

from urllib.parse import urlparse, parse_qs
from qgis.core import QgsVectorLayer


def get_results_directory(decoded_result):
    """
    Extract the results directory path from task JSON output.
    """
    parsed_url = urlparse(decoded_result["OUTPUT"]["href"])
    url_params = parse_qs(parsed_url.query)

    map_path = url_params.get("MAP", [None])[0]
    if not map_path:
        raise FileNotFoundError("MAP parameter missing from task output.")

    return os.path.dirname(map_path)


def get_vector_layer(results_dir):
    """
    Locate GeoPackage file in the results directory and load it as a vector layer.
    """
    vector_files = [f for f in os.listdir(results_dir) if f.lower().endswith(".gpkg")]
    if not vector_files:
        raise FileNotFoundError(f"No valid .gpkg file found in {results_dir}")

    vector_path = os.path.join(results_dir, vector_files[0])
    layer = QgsVectorLayer(vector_path, os.path.splitext(vector_files[0])[0], "ogr")
    if not layer.isValid():
        raise FileNotFoundError(f"Failed to load layer from {vector_path}")

    return layer


def get_postgres_connection():
    """
    Create and return a psycopg2 connection.
    """
    return psycopg2.connect(
        dbname=os.getenv("QGSWPS_DB_NAME"),
        host=os.getenv("QGSWPS_DB_HOST"),
        port=os.getenv("QGSWPS_DB_PORT"),
        user=os.getenv("QGSWPS_DB_USER"),
        password=os.getenv("QGSWPS_DB_PASSWORD"),
    )
