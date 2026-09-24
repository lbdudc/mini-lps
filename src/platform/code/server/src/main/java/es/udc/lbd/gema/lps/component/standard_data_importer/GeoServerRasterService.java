/*% if (feature.DM_DI_DF_GeoTIFF) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer;

import es.udc.lbd.gema.lps.config.GeoServerProperties;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.HTTPUtils;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Publishes the styles of the raster (GeoTIFF) layers.
 *
 * A raster is not a database table, so it is not published by GeoServerInit like the vector
 * layers: its coverage store is created when the file is uploaded (see
 * GeotiffFileImportService), and this applies to it the styles that layers.json lists for it.
 * A raster without styles keeps GeoServer's default raster style.
 */
@Component
public class GeoServerRasterService {

  private static final Logger logger = LoggerFactory.getLogger(GeoServerRasterService.class);

  private static final String LAYERS_FILE = "./src/main/resources/geoserver/layers.json";

  private final GeoServerProperties gsProp;

  public GeoServerRasterService(GeoServerProperties gsProp) {
    this.gsProp = gsProp;
  }

  /** Applies the styles layers.json lists for the raster layer with this name. */
  public void applyStyles(String layerName) {
    try {
      JSONObject layer = findRasterLayer(layerName);
      if (layer == null) {
        logger.info("Raster '{}' is not in layers.json, keeping the default style", layerName);
        return;
      }

      List<String> styles = new ArrayList<>();
      JSONArray available = (JSONArray) layer.get("availableStyles");
      if (available != null) {
        for (Object style : available) {
          if (styleExists((String) style)) {
            styles.add((String) style);
          } else {
            logger.warn("Style '{}' of raster '{}' does not exist in GeoServer", style, layerName);
          }
        }
      }
      if (styles.isEmpty()) {
        return;
      }

      String defaultStyle = (String) layer.get("defaultStyle");
      StringBuilder xml = new StringBuilder("<layer>");
      if (defaultStyle != null && styles.contains(defaultStyle)) {
        xml.append("<defaultStyle>").append(styleXml(defaultStyle)).append("</defaultStyle>");
      }
      xml.append("<styles>");
      for (String style : styles) {
        xml.append("<style>").append(styleXml(style)).append("</style>");
      }
      xml.append("</styles></layer>");

      String url = gsProp.getUrl() + "/rest/layers/" + gsProp.getWorkspace() + ":" + layerName;
      String response =
          HTTPUtils.put(url, xml.toString(), "text/xml", gsProp.getUser(), gsProp.getPassword());
      if (response != null) {
        logger.info("Styles of raster '{}' configured: {}", layerName, styles);
      } else {
        logger.warn("Could not configure the styles of raster '{}'", layerName);
      }
    } catch (Exception e) {
      logger.error("Error applying the styles of raster '" + layerName + "'", e);
    }
  }

  /**
   * Applies the styles again to the rasters GeoServer already has: the styles are created anew
   * every time the server starts, while the coverage stores survive in GeoServer's data
   * directory.
   */
  public void reapplyStyles(GeoServerRESTReader reader) {
    try {
      for (String layerName : rasterLayerNames()) {
        if (reader.existsCoverage(gsProp.getWorkspace(), layerName, layerName)) {
          applyStyles(layerName);
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  private List<String> rasterLayerNames() throws Exception {
    List<String> names = new ArrayList<>();
    for (Object layerObject : layersArray()) {
      JSONObject layer = (JSONObject) layerObject;
      if (isRaster(layer)) {
        for (Object subLayer : (JSONArray) ((JSONObject) layer.get("options")).get("layers")) {
          names.add(layerName((String) subLayer));
        }
      }
    }
    return names;
  }

  private JSONObject findRasterLayer(String layerName) throws Exception {
    for (Object layerObject : layersArray()) {
      JSONObject layer = (JSONObject) layerObject;
      if (isRaster(layer)) {
        for (Object subLayer : (JSONArray) ((JSONObject) layer.get("options")).get("layers")) {
          if (layerName((String) subLayer).equals(layerName)) {
            return layer;
          }
        }
      }
    }
    return null;
  }

  private JSONArray layersArray() throws Exception {
    JSONObject file = (JSONObject) new JSONParser().parse(new FileReader(LAYERS_FILE));
    return (JSONArray) file.get("layers");
  }

  private boolean isRaster(JSONObject layer) {
    return "wms".equals(layer.get("layerType")) && Boolean.TRUE.equals(layer.get("raster"));
  }

  /* a sublayer is written "workspace:name" */
  private String layerName(String subLayer) {
    return subLayer.substring(subLayer.indexOf(':') + 1);
  }

  private boolean styleExists(String style) {
    return HTTPUtils.exists(
        gsProp.getUrl() + "/rest/workspaces/" + gsProp.getWorkspace() + "/styles/" + style + ".xml",
        gsProp.getUser(),
        gsProp.getPassword());
  }

  private String styleXml(String style) {
    return "<name>" + escape(style) + "</name><workspace>" + escape(gsProp.getWorkspace()) + "</workspace>";
  }

  private String escape(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
/*% } %*/
