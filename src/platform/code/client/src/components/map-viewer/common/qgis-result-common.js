/*% if (feature.MV_Processes) { %*/
import { handleRequest, handleURL } from "@/common/proxy";
import {
  createGeoJSONLayer,
  createGeoTIFFLayer,
  getUniqueLayerId,
  createGeoJSONResultLayer
} from "@/components/map-viewer/common/map-layer-common";

const HANDLE_LAYER_MAP = {
  WFS: _handleVectorResult,
  WCS: _handleRasterResult,
};

/**
 * Creates layer from results from WPS QGIS job result
 */
async function handleResult(job, result, map) {
  if (_resultHasLayer(result)) {
    const service = result.SERVICE || (await _getResultSource(result));
    return HANDLE_LAYER_MAP[service](job, result, map);
  } else {
    console.warn(
      "Only layer-based results are supported to be displayed for now"
    );
    return null;
  }
}

/**
 * Returns true if the job result corresponds to a layer to be displayed
 */
function _resultHasLayer(result) {
  return result.OUTPUT && result.SERVICE;
}

/**
 * Creates vector layer from WPS QGIS job result
 */
function _handleVectorResult(job, result, map) {
  const [url, params] = _getLayerOutput(result);
  const layerId = params.get("layers");

  const getParams = {
    request: "GetFeature",
    service: "WFS",
    typename: layerId,
    outputformat: "application/json",
    map: params.get("MAP"),
  };

  const getUrl = unescape(url + L.Util.getParamString(getParams, url));
  /*const layer = createGeoJSONLayer(handleURL(getUrl, true), {
    name: job.jobID,
    label: layerId,
    added: true,
    type: "GEOJSON",
  });*/

  const layer = createGeoJSONResultLayer(
    { name: job.jobID, url: handleURL(getUrl, true) },
    {
      label: layerId,
      added: true,
      type: "GEOJSON",
    }
  );

  if (map && map.getLayer(job.jobID)) {
    console.warn("layer id already in use... generating new id");
    layer.options.id = getUniqueLayerId(map, job.jobID);
  }

  layer._initLayer();
  return layer;
}

/**
 * Creates raster layer from WPS QGIS job result
 */
async function _handleRasterResult(job, result, map) {
  const [url, params] = _getLayerOutput(result);
  const layerId = params.get("layers");

  const rasterParams = await _describeRasterResult(url, params);
  const getParams = {
    request: "GetCoverage",
    service: "WCS",
    coverage: layerId,
    crs: rasterParams.crs,
    bbox: rasterParams.bbox,
    width: rasterParams.grid[0],
    height: rasterParams.grid[1],
    format: "image/tiff",
    map: params.get("MAP"),
  };

  const getUrl = unescape(url + L.Util.getParamString(getParams, url));
  const layer = await createGeoTIFFLayer(
    { name: job.jobID, url: handleURL(getUrl, true) },
    {
      label: layerId,
      added: true,
      type: "GEOTIFF",
    }
  );

  if (map && map.getLayer(job.jobID)) {
    console.warn("layer id already in use... generating new id");
    layer.options.id = getUniqueLayerId(map, job.jobID);
  }

  return layer;
}

/**
 * Returns description of a raster layer (crs, bounding box and grid where [width, height])
 */
async function _describeRasterResult(url, params) {
  const describeParams = {
    request: "DescribeCoverage",
    service: "WCS",
    coverage: params.get("layers"),
    map: params.get("MAP"),
  };

  const xmlDoc = await handleRequest(
    unescape(url + L.Util.getParamString(describeParams, url))
  )
    .then((response) => response.text())
    .then((data) => {
      return new DOMParser().parseFromString(data, "application/xml");
    });

  return {
    crs: "EPSG:4326", // Default to EPSG:4326, GetCoverage does not work with the CRS in the describe response
    bbox: Array.from(xmlDoc.querySelectorAll("lonLatEnvelope pos"))
      .map((coord) => coord.textContent.split(" "))
      .join(","),
    grid: xmlDoc.querySelector("GridEnvelope high")?.textContent?.split(" "),
  };
}

/**
 * Returns OGC service where layer result will be available (fix for working when processes from models)
 */
async function _getResultSource(result) {
  const [url, params] = _getLayerOutput(result);

  const capabilitiesParams = {
    request: "GetCapabilities",
    service: "WCS",
    map: params.get("MAP"),
  };

  const xmlDoc = await handleRequest(
    unescape(url + L.Util.getParamString(capabilitiesParams, url))
  )
    .then((response) => response.text())
    .then((data) => {
      return new DOMParser().parseFromString(data, "application/xml");
    });

  // Check if result layer is available as a coverage
  const contentMetadata = xmlDoc.querySelector("ContentMetadata");
  const layerAsCoverage = Array.from(
    contentMetadata.querySelectorAll("CoverageOfferingBrief")
  ).some(
    (coverage) =>
      coverage.querySelector("name")?.textContent.trim() ===
      params.get("layers")
  );

  return layerAsCoverage ? "WCS" : "WFS";
}

function _getLayerOutput(result) {
  // Default layer output to 'OUTPUT'
  const outputName =
    Object.entries(result).find(
      ([, value]) =>
        typeof value === "object" && value.type?.startsWith("application/x-ogc")
    )?.[1].href || result?.OUTPUT;
  const outputUrl = new URL(outputName);

  return [`${outputUrl.origin}${outputUrl.pathname}`, outputUrl.searchParams];
}

export default handleResult;
/*% } %*/