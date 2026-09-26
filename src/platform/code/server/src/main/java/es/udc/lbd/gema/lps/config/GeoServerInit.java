/*% if (feature.MV_MS_GeoServer) { %*/
/*%
    var geographicEntities = data.dataModel.entities.filter(function(entity) {
        return checkEntityContainsPropertiesOfTypes(entity, geoTypes);
    });
%*/
package es.udc.lbd.gema.lps.config;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import it.geosolutions.geoserver.rest.encoder.datastore.GSPostGISDatastoreEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import it.geosolutions.geoserver.rest.manager.GeoServerRESTStoreManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import es.udc.lbd.gema.lps.model.service.util.GeoServerUtil;
/*% if (feature.DM_DI_DF_GeoTIFF) { %*/
import es.udc.lbd.gema.lps.component.standard_data_importer.GeoServerRasterService;
/*% } %*/

@Component
public class GeoServerInit {
    private final Logger logger = LoggerFactory.getLogger(GeoServerInit.class);

    @Inject
    private Properties prop;

    @Inject
    private GeoServerProperties gsProp;

    @Inject
    private GeoServerUtil gsUtil;

    /*% if (feature.DM_DI_DF_GeoTIFF) { %*/
    @Inject
    private GeoServerRasterService geoServerRasterService;
    /*% } %*/

    @Value("${spring.datasource.host}")
    private String pgHost;

    @Value("${spring.datasource.port}")
    private Integer pgPort;

    @Value("${spring.datasource.database}")
    private String pgDatabase;

    @Value("${spring.datasource.username}")
    private String pgUser;

    @Value("${spring.datasource.password}")
    private String pgPassword;

    private static final String SLDS_FOLDER = "geoserver/slds/";

    @PostConstruct
    public void init() {
        // If the property is not active, we don't do anything
        if (!gsProp.getActive()) {
          logger.info("GeoServer disabled.");
          return;
        }
        try {
            final GeoServerRESTReader reader = new GeoServerRESTReader(gsProp.getUrl(), gsProp.getUser(), gsProp.getPassword());
            final GeoServerRESTPublisher publisher = new GeoServerRESTPublisher(gsProp.getUrl(), gsProp.getUser(), gsProp.getPassword());
            final GeoServerRESTStoreManager manager = new GeoServerRESTStoreManager(new URL(gsProp.getUrl()), gsProp.getUser(), gsProp.getPassword());

            if (prop.getEnvironment().equals("dev")) {
              resetWorkspace(reader, publisher);
              Thread.sleep(
                3000); // geoserver-manager library is not async, so we wait for the workspace to be
              // removed
            } else {
              reloadWMSLayersBbox(); // We send a request to the GeoServer to reload the bbox of the WMS layers
            }
            createWorkspace(publisher);
            createDataStore(manager);

            // First we create all the styles in GeoServer that are of type WMSLayerStyle and cached
            // property is true
            HashSet<String> createdStyles = processStylesFile(publisher);
            // Then we create the layers and set as available styles that ones that are specificated in
            //  availableStyles array and has been created previously
            processLayersFile(publisher, createdStyles);
            /*% if (feature.DM_DI_DF_GeoTIFF) { %*/
            // The rasters uploaded before survive in GeoServer, the styles were just created again
            geoServerRasterService.reapplyStyles(reader);
            /*% } %*/

            if (prop.getEnvironment().equals("dev")) {
              Thread.sleep(3000); // wait for GeoServer to finish publishing before recalculating bbox
              reloadWMSLayersBbox();
            }

          /*% if (data.mapViewer == null) { %*/
            // Add a layer for each geographic entity
              /*% geographicEntities.forEach(function(entity) {
                  var type = "";
                  if (checkEntityContainsPropertiesOfTypes(entity, ["Point", "MultiPoint"])) {
                      type = "point";
                  } else if (checkEntityContainsPropertiesOfTypes(entity, ["Line", "MultiLineString"])) {
                      type = "line";
                  } else if (checkEntityContainsPropertiesOfTypes(entity, ["Polygon", "MultiPolygon"])) {
                      type = "polygon";
                  } %*/
            addLayer(publisher, "/*%= entity.name %*/", "t_/*%= entity.name.toLowerCase() %*/", "/*%= type %*/");
              /*% }); %*/
            /*% } %*/
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void resetWorkspace(GeoServerRESTReader reader, GeoServerRESTPublisher publisher) {
        /*% if (feature.DM_DI_DF_GeoTIFF) { %*/
        // The uploaded rasters can't be rebuilt from the database like the vector layers can:
        // removing the whole workspace would lose them, so only the datastore goes.
        if (!reader.getCoverageStores(gsProp.getWorkspace()).isEmpty()) {
          logger.debug("Removing datastore (the workspace has rasters)");
          publisher.removeDatastore(gsProp.getWorkspace(), gsProp.getDatastore(), true);
          return;
        }
        /*% } %*/
        logger.debug("Removing workspace");
        publisher.removeWorkspace(gsProp.getWorkspace(), true);
    }

    private void createWorkspace(GeoServerRESTPublisher publisher) {
        logger.debug("Creating workspace");
        publisher.createWorkspace(gsProp.getWorkspace());
    }

    private void createDataStore(GeoServerRESTStoreManager manager) {
        final GSPostGISDatastoreEncoder storeEncoder = new GSPostGISDatastoreEncoder(gsProp.getDatastore());
        storeEncoder.setHost(gsProp.getPgHost() != null ? gsProp.getPgHost() : pgHost);
        storeEncoder.setPort(pgPort);
        storeEncoder.setUser(pgUser);
        storeEncoder.setPassword(pgPassword);
        storeEncoder.setDatabase(pgDatabase);
        manager.create(gsProp.getWorkspace(), storeEncoder);
    }

  /*% if (data.mapViewer == null) { %*/
    private void addLayer(GeoServerRESTPublisher publisher, String title, String name, String type) {
        final GSFeatureTypeEncoder fte = new GSFeatureTypeEncoder();
        final GSLayerEncoder fse = new GSLayerEncoder();

        fte.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        fte.setTitle(title);
        fte.setName(name);
        fte.setSRS("EPSG:" + prop.getGis().getDefaultSrid());
        fte.setNativeCRS("EPSG:" + prop.getGis().getDefaultSrid());

        publisher.publishDBLayer(gsProp.getWorkspace(), gsProp.getDatastore(), fte, fse);
    }

  /*% } %*/
  private HashSet<String> processStylesFile(GeoServerRESTPublisher publisher) {
    HashSet<String> createdStyles = new HashSet<String>();
    try {
      JSONParser parser = new JSONParser();
      JSONObject fileObj =
        (JSONObject) parser.parse(new FileReader("./src/main/resources/geoserver/styles.json"));

      JSONArray styles = (JSONArray) fileObj.get("styles");

      for (Object styleObject : styles) {
        JSONObject style = (JSONObject) styleObject;
        String styleType = (String) style.get("type");
        Boolean styleCached = (Boolean) style.get("cached");
        if (styleCached != null && styleCached == true) {
          String name = (String) style.get("name");
          // One layer missing/unreadable SLD (e.g. a layer with no custom QGIS
          // style) must not abort every style after it in this list, so each
          // style is isolated rather than relying on the try/catch around the
          // whole loop.
          try {
            createdStyles.add(createStyle(publisher, name));
          } catch (Exception e) {
            logger.warn("Could not create GeoServer style '" + name + "', skipping it: " + e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return createdStyles;
  }

  // geoserver-manager's publishStyleInWorkspace() always POSTs as legacy SLD 1.0
  // (application/vnd.ogc.sld+xml). QGIS's own "Save as SLD" export uses the SE 1.1.0
  // vocabulary instead (se:Rule, se:Fill, se:SvgParameter...), which GeoServer's SLD 1.0
  // parser doesn't recognise — it silently drops every Fill/Stroke it can't parse, so the
  // style still gets created but renders with GeoServer's default grey fill. The
  // auto-generated random-colour style template (_WMSLayerSLDStyles.txt) is genuine SLD 1.0
  // and is unaffected, so only SE-tagged bodies need the different content type.
  private static final String SE_CONTENT_TYPE = "application/vnd.ogc.se+xml";

  private String createStyle(GeoServerRESTPublisher publisher, String name) {
    String fileName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    String stylePath = SLDS_FOLDER + fileName + ".sld";
    String sldBody = readFile(stylePath);
    // A redeploy keeps GeoServer's data (only a "dev" run resets the workspace), so the style
    // may already exist from an earlier deploy. Creating it then fails, and the look the
    // layer had back then would stay for good however its QGIS style changes: the style is
    // updated in place instead.
    String workspace = gsProp.getWorkspace();
    if (sldBody.contains("xmlns:se=")) {
      String stylesUrl = gsProp.getUrl() + "/rest/workspaces/" + workspace + "/styles";
      String created = HTTPUtils.post(stylesUrl + "?name=" + name, sldBody, SE_CONTENT_TYPE, gsProp.getUser(), gsProp.getPassword());
      if (created == null) {
        HTTPUtils.put(stylesUrl + "/" + name, sldBody, SE_CONTENT_TYPE, gsProp.getUser(), gsProp.getPassword());
      }
    } else if (!publisher.publishStyleInWorkspace(workspace, sldBody, name)) {
      publisher.updateStyleInWorkspace(workspace, sldBody, name);
    }
    return name;
  }

  private void processLayersFile(GeoServerRESTPublisher publisher, HashSet<String> createdStyles) {
    try {
      JSONParser parser = new JSONParser();
      JSONObject fileObj =
        (JSONObject) parser.parse(new FileReader("./src/main/resources/geoserver/layers.json"));

      JSONArray layers = (JSONArray) fileObj.get("layers");
      HashSet<String> createdLayers = new HashSet<String>();

      // First we get all the styles associated to each layer
      HashMap<String, HashSet> layersWithStyles =
        associateStylesWithSublayers(layers, createdStyles);

      for (Object layerObject : layers) {
        JSONObject layer = (JSONObject) layerObject;
        // A live layer reads a PostGIS table or a WFS layer somewhere else: GeoServer gets a store
        // of its own for it (nothing of it is in the app's database)
        if (layer.containsKey("live")) {
          publishLiveLayer(publisher, layer, layersWithStyles);
          continue;
        }
        // A raster is not a database table: its coverage is created when its file is uploaded
        if ("wms".equals((String) layer.get("layerType")) && !isRaster(layer)) {
          JSONObject options = (JSONObject) layer.get("options");
          JSONArray subLayers = (JSONArray) options.get("layers");

          String defaultStyle = (String) layer.get("defaultStyle");

          for (Object subLayer : subLayers) {
            // Each subLayer is specified as "workspace:table", so we split the string to get the
            // table's name
            String[] subLayerString = ((String) subLayer).split(":");
            if (!createdLayers.contains(subLayerString[1].toLowerCase())) {
              addLayer(
                publisher,
                layer,
                subLayerString[1].toLowerCase(),
                subLayerString[1].toLowerCase(),
                layersWithStyles.get(subLayer),
                defaultStyle);
              createdLayers.add(subLayerString[1].toLowerCase());
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return;
    }
  }

  /**
   * Method to create layers on GeoServer
   *
   * @param publisher
   * @param layer
   * @param layerName: name of the layer
   * @param tableName: name of the DB's table that we're going to recover the geometries
   */
  private void addLayer(
    GeoServerRESTPublisher publisher,
    JSONObject layer,
    String layerName,
    String tableName,
    HashSet<String> styles,
    String defaultStyle) {
    final GSFeatureTypeEncoder fte = new GSFeatureTypeEncoder();
    final GSLayerEncoder fse = new GSLayerEncoder();

    fte.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
    fte.setTitle(layerName);
    fte.setName(layerName);
    fte.setNativeName(tableName);
    fte.setSRS("EPSG:" + prop.getGis().getDefaultSrid());
    fte.setNativeCRS("EPSG:" + prop.getGis().getDefaultSrid());
    for (String style : styles) {
      fse.addStyle(style);
      if (style.equals(defaultStyle)) {
        fse.setDefaultStyle(style);
      }
    }

    boolean published = publisher.publishDBLayer(gsProp.getWorkspace(), gsProp.getDatastore(), fte, fse);
    if (!published) {
      // A deploy that failed half-way (a style GeoServer refused, say) can leave the feature
      // type behind without its layer, and publishing again then fails for good with
      // "already exists": the orphan is dropped and the layer published anew.
      try {
        GeoServerRESTReader reader = new GeoServerRESTReader(gsProp.getUrl(), gsProp.getUser(), gsProp.getPassword());
        if (!reader.existsLayer(gsProp.getWorkspace(), layerName, true)) {
          // unpublishFeatureType() starts by deleting the layer, which is what is missing here
          HTTPUtils.delete(
            gsProp.getUrl() + "/rest/workspaces/" + gsProp.getWorkspace() + "/datastores/" + gsProp.getDatastore()
              + "/featuretypes/" + layerName + "?recurse=true",
            gsProp.getUser(), gsProp.getPassword());
          publisher.publishDBLayer(gsProp.getWorkspace(), gsProp.getDatastore(), fte, fse);
        }
      } catch (Exception e) {
        logger.warn("Could not republish the GeoServer layer '" + layerName + "': " + e.getMessage());
      }
    }
  }

  /**
   * Publishes a live layer: a store for its source (a PostGIS database or a WFS service), the feature
   * type, and the QGIS style. One failing source must not stop the others or the server.
   */
  private void publishLiveLayer(
    GeoServerRESTPublisher publisher, JSONObject layer, HashMap<String, HashSet> layersWithStyles) {
    JSONObject live = (JSONObject) layer.get("live");
    String layerName = String.valueOf(live.get("layerName")).toLowerCase();
    try {
      String kind = String.valueOf(live.get("kind"));
      String workspace = gsProp.getWorkspace();
      String storeUrl = gsProp.getUrl() + "/rest/workspaces/" + workspace + "/datastores";
      String store = layerName;

      String storeXml;
      String nativeName;
      if ("postgis".equals(kind)) {
        storeXml = liveStoreXml(store, "PostGIS", new String[][] {
          {"dbtype", "postgis"},
          {"host", String.valueOf(live.get("host"))},
          {"port", String.valueOf(live.get("port"))},
          {"database", String.valueOf(live.get("database"))},
          {"schema", String.valueOf(live.get("schema"))},
          {"user", String.valueOf(live.get("user"))},
          {"passwd", String.valueOf(live.get("password"))},
          {"Expose primary keys", "true"},
          {"validate connections", "true"},
        });
        nativeName = String.valueOf(live.get("table"));
      } else if ("wfs".equals(kind)) {
        String user = String.valueOf(live.get("user"));
        java.util.List<String[]> params = new ArrayList<String[]>();
        params.add(new String[] {"WFSDataStoreFactory:GET_CAPABILITIES_URL", String.valueOf(live.get("url"))});
        if (!user.isEmpty()) {
          params.add(new String[] {"WFSDataStoreFactory:USERNAME", user});
          params.add(new String[] {"WFSDataStoreFactory:PASSWORD", String.valueOf(live.get("password"))});
        }
        params.add(new String[] {"WFSDataStoreFactory:TIMEOUT", "30000"});
        storeXml = liveStoreXml(store, "Web Feature Server (NG)", params.toArray(new String[0][]));
        // GeoServer names the types of a WFS store prefix_name
        nativeName = String.valueOf(live.get("typeName")).replace(':', '_');
      } else {
        logger.error("Live layer '" + layerName + "': unknown source kind '" + kind + "'");
        return;
      }

      // A redeploy keeps GeoServer's data: the store may be there already, then it is updated
      String created = HTTPUtils.post(storeUrl, storeXml, "text/xml", gsProp.getUser(), gsProp.getPassword());
      if (created == null) {
        HTTPUtils.put(storeUrl + "/" + store, storeXml, "text/xml", gsProp.getUser(), gsProp.getPassword());
      }

      long srid = live.get("srid") instanceof Number ? ((Number) live.get("srid")).longValue() : 4326L;
      final GSFeatureTypeEncoder fte = new GSFeatureTypeEncoder();
      final GSLayerEncoder fse = new GSLayerEncoder();
      fte.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
      fte.setTitle(layerName);
      fte.setName(layerName);
      fte.setNativeName(nativeName);
      fte.setSRS("EPSG:" + srid);
      fte.setNativeCRS("EPSG:" + srid);

      String defaultStyle = (String) layer.get("defaultStyle");
      JSONArray subLayers = (JSONArray) ((JSONObject) layer.get("options")).get("layers");
      HashSet<String> styles = layersWithStyles.get((String) subLayers.get(0));
      if (styles != null) {
        for (String style : styles) {
          fse.addStyle(style);
          if (style.equals(defaultStyle)) {
            fse.setDefaultStyle(style);
          }
        }
      }

      boolean published = publisher.publishDBLayer(workspace, store, fte, fse);
      if (!published) {
        // Left behind by an earlier deploy: dropped and published anew
        HTTPUtils.delete(storeUrl + "/" + store + "/featuretypes/" + layerName + "?recurse=true",
          gsProp.getUser(), gsProp.getPassword());
        published = publisher.publishDBLayer(workspace, store, fte, fse);
      }
      if (!published) {
        logger.error("Could not publish the live layer '" + layerName + "' from " + kind + " (check its address and credentials)");
        return;
      }
      HTTPUtils.put(
        storeUrl + "/" + store + "/featuretypes/" + layerName + "?recalculate=nativebbox,latlonbbox",
        "<featureType><enabled>true</enabled></featureType>",
        "application/xml", gsProp.getUser(), gsProp.getPassword());
      logger.info("Live layer published: " + workspace + ":" + layerName + " (" + kind + ")");
    } catch (Exception e) {
      logger.error("Could not publish the live layer '" + layerName + "': " + e.getMessage(), e);
    }
  }

  private static String liveStoreXml(String name, String type, String[][] entries) {
    StringBuilder xml = new StringBuilder("<dataStore><name>")
      .append(xmlText(name)).append("</name><type>").append(xmlText(type))
      .append("</type><enabled>true</enabled><connectionParameters>");
    for (String[] entry : entries) {
      xml.append("<entry key=\"").append(xmlText(entry[0])).append("\">")
        .append(xmlText(entry[1])).append("</entry>");
    }
    return xml.append("</connectionParameters></dataStore>").toString();
  }

  private static String xmlText(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  /**
   * It goes through the whole array of layers and associates, to each subLayer, all the styles that will be available.
   * Returns a map whose key is the name of the subLayer and value is a set of available styles of this one.
   * @param layers: array of layers from the config file "layers.json"
   * @param createdStyles: set of cached styles (that has been created previously in the GeoServer)
   * @return map of layers with the available styles for each one
   */
  private HashMap<String, HashSet> associateStylesWithSublayers(
    JSONArray layers, HashSet<String> createdStyles) {
    HashMap<String, HashSet> layersWithStyles = new HashMap<String, HashSet>();
    for (Object layerObject : layers) {
      JSONObject layer = (JSONObject) layerObject;
      if ("wms".equals((String) layer.get("layerType"))) {
        JSONObject options = (JSONObject) layer.get("options");
        JSONArray subLayers = (JSONArray) options.get("layers");
        List<String> layerStyles =
          layer.containsKey("availableStyles")
            ? (JSONArray) layer.get("availableStyles")
            : new ArrayList();
        layerStyles =
          layerStyles.stream()
            .filter(layerStyle -> createdStyles.contains(layerStyle))
            .collect(Collectors.toList());
        for (Object subLayer : subLayers) {
          String subLayerStr = (String) subLayer;
          if (!layersWithStyles.containsKey(subLayerStr)) {
            layersWithStyles.put(subLayerStr, new HashSet<String>());
          }
          for (String style : layerStyles) {
            layersWithStyles.get(subLayerStr).add(style);
          }
        }
      }
    }
    return layersWithStyles;
  }

  private boolean isRaster(JSONObject layer) {
    return Boolean.TRUE.equals(layer.get("raster"));
  }

  private void reloadWMSLayersBbox() {
    try {
      JSONParser parser = new JSONParser();
      JSONObject fileObj =
        (JSONObject) parser.parse(new FileReader("./src/main/resources/geoserver/layers.json"));

      JSONArray layers = (JSONArray) fileObj.get("layers");

      for (Object layerObject : layers) {
        JSONObject layer = (JSONObject) layerObject;
        if ("wms".equals((String) layer.get("layerType")) && !isRaster(layer) && !layer.containsKey("live")) {
          JSONObject options = (JSONObject) layer.get("options");
          JSONArray subLayers = (JSONArray) options.get("layers");

          for (Object subLayer : subLayers) {
            String[] subLayerString = ((String) subLayer).split(":");
            gsUtil.recalculateFeatureTypeBBox(subLayerString[1].toLowerCase());
          }
        }
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
    }
  }

  private String readFile(String fileName) {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream inputStream = classloader.getResourceAsStream(fileName);
    if (inputStream == null) {
      // A layer with no custom QGIS style never had an SLD to bundle here — that's
      // expected, not an error worth a stack trace; the caller decides what to do
      // with an empty body.
      logger.warn("Resource not found on classpath: " + fileName);
      return "";
    }

    try {
      StringBuilder strbld = new StringBuilder();
      InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      BufferedReader reader = new BufferedReader(streamReader);
      for (String line; (line = reader.readLine()) != null; ) {
        strbld.append(line);
      }
      return strbld.toString();

    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }

    return "";
  }

}
/*% } %*/
