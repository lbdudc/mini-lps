#%@
if (!feature.MV_Processes) return [];
return [{ fileName: fileName, basePath: basePath, context: context }];
%#
import re
import os
import requests
import tempfile

from urllib.parse import urlparse
from osgeo import ogr
from qgis import processing
from qgis.core import QgsVectorLayer, QgsRasterLayer


class LayerFactory:
    """
    Factory to create QGIS layer
    """

    def __init__(self, feedback):
        self.feedback = feedback

    def init_layer(self, layer_options):
        """Creates QGIS layer based on passed options"""
        layer_type = layer_options["type"]
        layer_creators = {
            "ARCGIS": self.init_arcgis,
            "GEOJSON": self.init_vector_layer,
            "GEOPACKAGE": self.init_geopackage,
            "GEOPARQUET": self.init_vector_layer,
            "GEOTIFF": self.init_geotiff_layer,
            "SHAPEFILE": self.init_shapefile,
            "WCS": self.init_wcs_layer,
            "WFS": self.init_vector_layer,
            "WMS": self.init_wms_as_wfs,
            "POSTGRES": self.init_db_layer,
        }

        layer_creator = layer_creators.get(layer_type)
        if not layer_creator:
            self.feedback.pushInfo(f"Unsupported {layer_type} layer")
            return None

        layer = layer_creator(layer_options)
        if not layer.isValid():
            self.feedback.pushInfo(f"Failed to load {layer_type} layer")
            return None

        return layer

    def init_vector_layer(self, layer_options):
        """
        Instantiate a vector layer from direct source
        """
        source = layer_options["url"]

        return QgsVectorLayer(source, layer_options["id"], "ogr")

    def init_arcgis(self, layer_options):
        """
        Instantiate a vector layer from ArcGIS source
        """
        source = layer_options["url"]
        arcgis_source = source + "/query?f=pgeojson&where=1=1&outFields=*"

        return QgsVectorLayer(arcgis_source, layer_options["id"], "ogr")

    def init_geopackage(self, layer_options):
        """
        Instantiate a vector layer from GeoPackage source
        """
        source = layer_options["url"]

        dataset = ogr.Open(source)
        if not dataset or dataset.GetLayerCount() == 0:
            return None

        layer_name = dataset.GetLayer(0).GetName()
        gpkg_source = f"{source}|layername={layer_name}"

        return QgsVectorLayer(gpkg_source, layer_options["id"], "ogr")

    def init_shapefile(self, layer_options):
        """
        Instantiate a vector layer from Shapefile source
        """
        source = layer_options["url"]

        if self._is_url(source):
            temp_file = self._create_temp_file(source, layer_options["map"], ".zip")
            if temp_file:
                shapefile_source = f"/vsizip/{temp_file.name}"
                return QgsVectorLayer(shapefile_source, layer_options["id"], "ogr")
            else:
                return None
        else:
            return QgsVectorLayer(source, layer_options["id"], "ogr")

    def init_geotiff_layer(self, layer_options):
        """
        Instantiate a raster layer from GeoTIFF source
        """
        if isinstance(layer_options["url"], list):
            source_list = layer_options["url"]

            if len(source_list) > 1:
                temp_files = [
                    self._create_temp_file(url, layer_options["map"], ".tif")
                    for url in source_list
                ]
                result = processing.run(
                    "gdal:merge",
                    {
                        "INPUT": [file.name for file in temp_files],
                        "PCT": False,
                        "SEPARATE": False,
                        "NODATA_INPUT": None,
                        "NODATA_OUTPUT": -9999,
                        "OPTIONS": None,
                        "DATA_TYPE": 5,
                        "OUTPUT": "TEMPORARY_OUTPUT",
                    },
                )
                return QgsRasterLayer(result["OUTPUT"], layer_options["id"], "gdal")
            else:
                tiff_source = f"/vsicurl/{source_list[0]}"
                return QgsRasterLayer(tiff_source, layer_options["id"], "gdal")
        else:
            return QgsRasterLayer(layer_options["url"], layer_options["id"], "gdal")

    def init_wcs_layer(self, layer_options):
        """
        Instantiate a raster layer from WCS source
        """
        wcs_source = f"WCS:?SERVICE=WCS&VERSION=1.0.0&REQUEST=GetCoverage&url={layer_options['url']}&identifier={layer_options['id']}"

        return QgsRasterLayer(wcs_source, layer_options["id"], "wcs")

    def init_db_layer(self, layer_options):
        """
        Instantiate a vector layer from a known database
        """
        table_name, table_geom = layer_options["id"].rsplit("-", 1)
        db_source = (
            f"dbname='{os.getenv('QGSWPS_DB_NAME')}' "
            f"host='{os.getenv('QGSWPS_DB_HOST')}' "
            f"port='{os.getenv('QGSWPS_DB_PORT')}' "
            f"user='{os.getenv('QGSWPS_DB_USER')}' "
            f"password='{os.getenv('QGSWPS_DB_PASSWORD')}' "
            f'table="public"."t_{table_name}" ({table_geom})'
        )

        return QgsVectorLayer(db_source, layer_options["id"], "postgres")

    def init_wms_as_wfs(self, layer_options):
        """
        Load a WMS-backed layer as WFS. Downloads GeoJSON to /projects/{map_id}/
        so the files persist when the project is reloaded for process execution.
        """
        geoserver_url = os.getenv("QGSWPS_GEOSERVER_URL")
        if not geoserver_url:
            self.feedback.pushInfo(
                f"QGSWPS_GEOSERVER_URL not set, cannot load WMS layer {layer_options['id']} as WFS"
            )
            return QgsVectorLayer("", layer_options["id"], "ogr")

        sublayers = layer_options.get("params", {}).get("layers", [])
        if not sublayers:
            return QgsVectorLayer("", layer_options["id"], "ogr")

        typename = sublayers[0]
        wfs_url = (
            f"{geoserver_url}/wfs"
            f"?service=WFS&version=2.0.0&request=GetFeature"
            f"&typeNames={typename}&outputFormat=application/json"
        )

        self.feedback.pushInfo(f"Fetching WFS: {wfs_url}")
        response = requests.get(wfs_url, timeout=30)
        if response.status_code != 200:
            self.feedback.pushInfo(
                f"WFS request failed with status {response.status_code} for {typename}"
            )
            return QgsVectorLayer("", layer_options["id"], "ogr")

        map_id = layer_options.get("map", "tmp")
        layer_dir = f"/projects/{map_id}"
        os.makedirs(layer_dir, exist_ok=True)
        layer_path = os.path.join(layer_dir, f"{layer_options['id']}.geojson")
        with open(layer_path, "wb") as f:
            f.write(response.content)

        return QgsVectorLayer(layer_path, layer_options["id"], "ogr")

    def _create_temp_file(self, source, map_id, suffix):
        """
        Creates a temporary file
        """
        response = requests.get(source, stream=True)
        if response.status_code == 200:
            temp_dir = os.path.join(tempfile.gettempdir(), str(map_id))
            os.makedirs(temp_dir, exist_ok=True)

            temp_file = tempfile.NamedTemporaryFile(
                delete=False, suffix=suffix, dir=temp_dir
            )
            temp_file.write(response.content)
            temp_file.close()

            return temp_file
        else:
            return None

    def _is_url(self, source):
        """
        Check if the given source string is an HTTP/HTTPS URL
        """
        try:
            result = urlparse(source)
            return result.scheme in ("http", "https")
        except:
            return False
