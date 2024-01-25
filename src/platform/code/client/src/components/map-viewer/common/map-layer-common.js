/*% if (feature.MapViewer || feature.MV_LM_ExternalLayer) { %*/
/**
 * Common functions to create and modify layers
 */
/*% if (feature.MV_MS_GeoServer) { %*/
import properties from "@/properties";
/*% } %*/
import RepositoryFactory from "@/repositories/RepositoryFactory";
/*% if (feature.MV_MS_GJ_Paginated) { %*/
import layers from "../config-files/layers.json";
/*% } %*/
import { getStyle } from "@/components/map-viewer/common/map-styles-common";
import { GeoJSONLayer, WMSLayer } from "@lbdudc/map-viewer";
/**
 * Creates a WMS Layer.
 */
function createWMSLayer(json, layerParams, layerInMap = {}, /*% if (feature.MV_T_F_BasicSearch) { %*/form = {}/*% } %*/) {
  const options =
    /*% if (feature.MV_T_F_BasicSearch) { %*/
    (form.query == null || form.query === "")
      ? json.options
      : Object.assign(
        {
          cql_filter:
            "in (" +
            form.query +
            ")"
        },
        json.options
      );
  /*% } else { %*/
  json.options;
  /*% } %*/

  const availableStyles = _getAvailableStyles(json);
  const defaultStyle = _getDefaultStyle(json, availableStyles, layerInMap);

  return new WMSLayer(
    {
      id: json.name,
      label: layerParams.label,
      baseLayer: false,
      selected: layerInMap.selected || layerInMap.selected == null, // if no value is given, it is shown in map
      /*% if (feature.MV_T_ViewMapAsList) { %*/
      list: layerInMap.list || json.list || null,
      /*% } %*/
      url: json.url/*% if (feature.MV_MS_GeoServer) { %*/ || properties.GEOSERVER_URL + "/wms"/*% } %*/,
      params: options,
      added: layerParams.added,
    },
    availableStyles,
    defaultStyle
  );
}

/**
 * Creates a GeoJSON Layer.
 */
function createGeoJSONLayer(json, layerParams, layerInMap = {}/*% if (feature.MV_T_F_BasicSearch) { %*/, form = {}/*% } %*/) {
    const repository = RepositoryFactory.get(
      _getRepositoryNameFromJSON(json)
    );

    /*% if (feature.MV_MS_GJ_Paginated) { %*/
    const options = _getBBoxPagination(layerParams.bounds);
    /*% } %*/
    /*% if (feature.MV_DetailOnClick) { %*/
    const popup = layerParams.hasOwnProperty("popupFn")
      ? layerParams.popupFn(layerInMap.form || json.form)
      : null;
    /*% } %*/
    const availableStyles = _getAvailableStyles(json);
    const defaultStyle = _getDefaultStyle(json, availableStyles, layerInMap);
    /*% if (feature.MV_MS_GJ_Cached) { %*/
    const repositoryName = _getRepositoryNameFromJSON(json);
    const fileName = repositoryName.includes("Entity")
        ? _getEntityNameFromJSON(json)
        : _getPropertyNameFromJSON(json);
    /*% } %*/

    return new GeoJSONLayer(
        repository.getGeom(
            /*% if (feature.MV_MS_GJ_Cached) { %*/fileName, /*% } else { %*/_getPropertyNameFromJSON(json), /*% } %*/
            /*% if (feature.MV_MS_GJ_Paginated) { %*/ options), /*% } else { %*/
            {
                params: {
                    properties: true,
                    /*% if (feature.MV_T_F_BasicSearch) { %*/
                    search: form.query
                    /*% } %*/
                }
            }),
            /*% } %*/
        {
            id: json.name,
            label: layerParams.label,
            baseLayer: false,
            selected: layerInMap.selected || layerInMap.selected == null,  // if no value is given, it is shown in map
            /*% if (feature.MV_Clustering) { %*/
            clustering: true,
            /*% } %*/
            /*% if (feature.MV_T_ViewMapAsList) { %*/
            list: layerInMap.list || json.list,
            /*% } %*/
            url: json.url,
            /*% if (feature.MV_DetailOnClick) { %*/
            popup: popup,
            /*% } %*/
            added: layerParams.added,
        },
        availableStyles,
        defaultStyle,
    );
}

/*% if (feature.MV_MS_GJ_Paginated) { %*/
/**
 * Updates GeoJSON layer features within a given bounding box.
 */
function updateLayer(map, bbox, popupFn) {
  const layersToShow = layers.layers.filter(
    (layer) => layer.layerType === "geojson" && map.getLayer(layer.name)
  );

  const options = _getBBoxPagination(bbox);

  layersToShow.forEach((json) => {
    const repository = RepositoryFactory.get(
      _getRepositoryNameFromJSON(json)
    );

    repository
      .getGeom(_getPropertyNameFromJSON(json), options)
      .then((data) => {
        if (data.features.length !== 0) {
          let layer = map.getLayer(json.name);
          layer
            /*% if (feature.MV_Clustering) { %*/
            ._getRealLayer()
            /*% } else { %*/
            .getLayer()
            /*% } %*/
            .then((layr) => {
              /*% if (feature.MV_Clustering) { %*/
              layer.getLayer().then((cluster) => {
                cluster.clearLayers();
                cluster.addLayer(_updateLayerData(layr, data));
              });
              /*% } else { %*/
              _updateLayerData(layr, data);
              /*% } %*/
              /*% if (feature.MV_DetailOnClick) { %*/
            })
            .finally(() => {
              layer.bindPopup(popupFn(json.form));
              /*% } %*/
            });
        }
      });
  });
}

function _updateLayerData(layer, data) {
  if (Object.keys(layer._layers).length === 0) {
    layer.addData(data);
  } else {
    const dataObj = {};
    data.features.forEach((item) => (dataObj[item.id] = item));
    layer.eachLayer((subLayer) => {
      const found = dataObj[subLayer.feature.id];
      if (found) {
        delete dataObj[subLayer.feature.id];
      } else {
        layer.removeLayer(subLayer);
      }
    });

    const newFeatures = Object.values(dataObj);
    layer.addData(newFeatures);
  }
  /*% if (feature.MV_Clustering) { %*/
  return layer;
  /*% } %*/
}
/*% } %*/

function _getAvailableStyles(json) {
  return json.availableStyles?.map((availableStyleName) =>
    getStyle(availableStyleName)
  );
}

function _getDefaultStyle(json, availableStyles, layerInMap) {
  return layerInMap.style != null
    ? availableStyles.find((style) => style.id === layerInMap.style)
    : json.defaultStyle != null
      ? availableStyles.find((style) => style.id === json.defaultStyle)
      : null;
}


function _getEntityNameFromJSON(json) {
  const prop = json.entityName != null ? "entityName" : "name";
  const nameParts = json[prop].split("-");
  if (nameParts.length > 2) {
    return (
      nameParts[0] +
      "-" +
      nameParts[1].charAt(0).toUpperCase() +
      nameParts[1].slice(1)
    );
  }
  return nameParts[0];
}

function _getRepositoryNameFromJSON(json) {
  let entityName = _getEntityNameFromJSON(json);
  let repositorySuffix = "EntityRepository";
  // Check if component entity
  if (entityName.indexOf("-") != -1) {
    repositorySuffix = "Repository";
    entityName = entityName.replace("-", "");
  }
  return (
    entityName.charAt(0).toUpperCase() + entityName.slice(1) + repositorySuffix
  );
}

function _getPropertyNameFromJSON(json) {
  // returns last substring after a '-'
  const prop = json.entityName != null ? "entityName" : "name";
  const nameParts = json[prop].split("-");
  return nameParts[nameParts.length - 1];
}

/*% if (feature.MV_MS_GJ_Paginated) { %*/
function _getBBoxPagination(bounds) {
  const augmentedBBox = _incrementBBox(
    bounds.getWest(),
    bounds.getEast(),
    bounds.getSouth(),
    bounds.getNorth()
  );

  const options = { params: {} };
  options.params.xmin = augmentedBBox.xmin;
  options.params.xmax = augmentedBBox.xmax;
  options.params.ymin = augmentedBBox.ymin;
  options.params.ymax = augmentedBBox.ymax;

  return options;
}

function _incrementBBox(xmin, xmax, ymin, ymax) {
  let incrementNS = (ymax - ymin) * 0.3;
  let incrementEW = (xmax - xmin) * 0.3;

  let augmentedXmin = xmin - incrementEW;
  let augmentedXmax = xmax + incrementEW;
  let augmentedYmin = ymin - incrementNS;
  let augmentedYmax = ymax + incrementNS;

  return {
    xmin: augmentedXmin,
    xmax: augmentedXmax,
    ymin: augmentedYmin,
    ymax: augmentedYmax,
  };
}
/*% } %*/

export { createWMSLayer, createGeoJSONLayer/*% if (feature.MV_MS_GJ_Paginated) { %*/, updateLayer/*% } %*/ };
/*% } %*/
