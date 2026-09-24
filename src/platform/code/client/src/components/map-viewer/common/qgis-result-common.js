/*% if (feature.MV_Processes) { %*/
import { handleRequest, handleURL } from "@/common/proxy";
import {
  getUniqueLayerId,
  createGeoJSONResultLayer
} from "@/components/map-viewer/common/map-layer-common";

/**
 * Creates the layers for a WPS QGIS job result. A job can have several layer
 * outputs (a QGIS model with two sinks, for instance), so this returns an
 * array with one layer per output — empty if there is nothing displayable.
 */
async function handleResult(job, result, map) {
  const outputs = _getLayerOutputs(result);
  if (outputs.length === 0) {
    console.warn(
      "Only layer-based results are supported to be displayed for now"
    );
    return [];
  }

  const layers = [];
  for (const output of outputs) {
    const [url, params] = _splitOutputHref(output.href);
    const service = result.SERVICE || (await _getResultSource(url, params));

    if (service !== "WFS") {
      console.warn(
        `Raster (${service}) results can not be displayed yet: ${output.key}`
      );
      continue;
    }

    // A single output keeps the job id as its layer id, several get a suffix
    const id = outputs.length === 1 ? job.jobID : `${job.jobID}-${output.key}`;
    layers.push(_handleVectorResult({ ...job, jobID: id }, url, params, output, map));
  }
  return layers;
}

/**
 * Returns the layer outputs of a job result as [{ key, title, href }]
 */
function _getLayerOutputs(result) {
  const outputs = Object.entries(result)
    .filter(
      ([, value]) =>
        value &&
        typeof value === "object" &&
        value.href &&
        value.type?.startsWith("application/x-ogc")
    )
    .map(([key, value]) => ({ key, title: value.title, href: value.href }));

  // Default layer output to 'OUTPUT'
  if (outputs.length === 0 && typeof result.OUTPUT === "string") {
    outputs.push({ key: "OUTPUT", title: null, href: result.OUTPUT });
  }
  return outputs;
}

function _splitOutputHref(href) {
  const outputUrl = new URL(href);
  return [`${outputUrl.origin}${outputUrl.pathname}`, outputUrl.searchParams];
}

/**
 * Creates vector layer from WPS QGIS job result
 */
function _handleVectorResult(job, url, params, output, map) {
  const layerId = params.get("layers");

  const getParams = {
    request: "GetFeature",
    service: "WFS",
    typename: layerId,
    outputformat: "application/json",
    srsname: "EPSG:4326", /* Leaflet needs lon/lat, whatever CRS the model ran in */
    map: params.get("MAP"),
  };

  const getUrl = unescape(url + L.Util.getParamString(getParams, url));

  const layer = createGeoJSONResultLayer(
    { name: job.jobID, url: handleURL(getUrl, true) },
    {
      label: output.title || layerId,
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
 * Returns OGC service where layer result will be available (fix for working when processes from models)
 */
async function _getResultSource(url, params) {
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
    contentMetadata?.querySelectorAll("CoverageOfferingBrief") || []
  ).some(
    (coverage) =>
      coverage.querySelector("name")?.textContent.trim() ===
      params.get("layers")
  );

  return layerAsCoverage ? "WCS" : "WFS";
}

export default handleResult;
/*% } %*/