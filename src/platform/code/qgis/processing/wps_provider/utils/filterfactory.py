#% if (feature.MV_Processes) { %#
import os
import tempfile

from collections import defaultdict
from qgis.core import QgsMapLayer, QgsRasterLayer, QgsVectorLayer
from qgis.analysis import QgsRasterCalculator, QgsRasterCalculatorEntry


class FilterFactory:
    """Factory for creating and applying filters for QGIS layers"""

    OPERATOR_MAP = {
        "GREATER": ">",
        "GREATER_EQUAL": ">=",
        "LESS": "<",
        "LESS_EQUAL": "<=",
        "EQUAL": "=",
        "DIFFERENT": "!=",
    }

    def __init__(self, feedback):
        self.feedback = feedback

    def apply_filter(self, layer, filters) -> QgsMapLayer | None:
        """
        Creates and applies a subset filter to a QGIS layer
        """
        filter_creators = {
            QgsMapLayer.VectorLayer: self._apply_vector_filters,
            QgsMapLayer.RasterLayer: self._apply_raster_filters,
        }

        filter_creator = filter_creators.get(layer.type())
        if not filter_creator:
            self.feedback.pushInfo(f"Layer {layer.type()} does not support filtering")
            return None

        return filter_creator(layer, filters)

    def _apply_vector_filters(self, layer, filters) -> QgsVectorLayer:
        """
        Applies a subset filter to a QGIS vector layer
        """
        conditions = []
        for layer_filter in filters:
            field = layer_filter.get("property")
            value = layer_filter.get("value")
            comparator = self.OPERATOR_MAP.get(layer_filter.get("comparator"), "=")

            # handle strings and null values
            if isinstance(value, str):
                value = f"'{value}'"
            elif value is None:
                value = "NULL"

            conditions.append(f'"{field}" {comparator} {value}')

        # Apply filters to layer
        filter_string = " AND ".join(conditions)
        layer.setSubsetString(filter_string)

        return layer

    def _apply_raster_filters(self, layer, filters) -> QgsRasterLayer:
        """
        Applies a subset filter to a QGIS raster layer
        """
        band_conditions = defaultdict(list)
        for layer_filter in filters:
            band = layer_filter.get("property").split()[1]
            value = layer_filter.get("value")
            comparator = self.OPERATOR_MAP.get(layer_filter.get("comparator"), "=")

            band_conditions[band].append(
                f'("{layer.name()}@{band}" {comparator} {value})'
            )

        band_expressions = []
        for band, conditions in band_conditions.items():
            band_expressions.append(
                f"({' AND '.join(conditions)}) * {layer.name()}@{band}"
            )

        # Prepare QgsRasterCalculator entries
        entries = []
        for band in band_conditions.keys():
            entry = QgsRasterCalculatorEntry()
            entry.ref = f"{layer.name()}@{band}"
            entry.raster = layer
            entry.bandNumber = int(band)
            entries.append(entry)

        # Output temporary file
        output_path = os.path.join(
            tempfile.gettempdir(), f"filtered_{layer.name()}.tif"
        )

        # Apply filters to new layer file
        calculator = QgsRasterCalculator(
            str(band_expressions),
            output_path,
            "GTiff",
            layer.extent(),
            layer.width(),
            layer.height(),
            entries,
        )
        calculator.processCalculation()

        # Create new filtered layer
        return QgsRasterLayer(output_path, f"{layer.name()}", "gdal")

#% } %#
