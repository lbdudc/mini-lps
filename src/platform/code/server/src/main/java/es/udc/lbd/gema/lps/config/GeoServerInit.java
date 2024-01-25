/*% if (feature.MV_MS_GeoServer) { %*/
/*%
    var geographicEntities = data.dataModel.entities.filter(function(entity) {
        return checkEntityContainsPropertiesOfTypes(entity, geoTypes);
    });
%*/
package es.udc.lbd.gema.lps.config;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
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

@Component
public class GeoServerInit {
    private final Logger logger = LoggerFactory.getLogger(GeoServerInit.class);

    @Inject
    private Properties prop;

    @Inject
    private GeoServerProperties gsProp;

    @Inject
    private GeoServerUtil gsUtil;

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

    private static final String SLDS_FOLDER = "./geoserver/slds/";

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
              removeWorkspace(publisher);
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

    private void removeWorkspace(GeoServerRESTPublisher publisher) {
        logger.debug("Removing workspace");
        publisher.removeWorkspace(gsProp.getWorkspace(), true);
    }

    private void createWorkspace(GeoServerRESTPublisher publisher) {
        logger.debug("Creating workspace");
        publisher.createWorkspace(gsProp.getWorkspace());
    }

    private void createDataStore(GeoServerRESTStoreManager manager) {
        final GSPostGISDatastoreEncoder storeEncoder = new GSPostGISDatastoreEncoder(gsProp.getDatastore());
        storeEncoder.setHost(pgHost);
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
          createdStyles.add(
            createStyle(publisher, (String) style.get("name")));
        }
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return createdStyles;
  }

  private String createStyle(GeoServerRESTPublisher publisher, String name) {
    String stylePath = SLDS_FOLDER + name + ".sld";
    String sldBody = readFile(stylePath);
    publisher.publishStyleInWorkspace(gsProp.getWorkspace(), sldBody, name);
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
        if ("wms".equals((String) layer.get("layerType"))) {
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

    publisher.publishDBLayer(gsProp.getWorkspace(), gsProp.getDatastore(), fte, fse);
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

  private void reloadWMSLayersBbox() {
    try {
      JSONParser parser = new JSONParser();
      JSONObject fileObj =
        (JSONObject) parser.parse(new FileReader("./src/main/resources/geoserver/layers.json"));

      JSONArray layers = (JSONArray) fileObj.get("layers");

      for (Object layerObject : layers) {
        JSONObject layer = (JSONObject) layerObject;
        if ("wms".equals((String) layer.get("layerType"))) {
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
    try {
      StringBuilder strbld = new StringBuilder();
      ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      InputStream inputStream = classloader.getResourceAsStream(fileName);
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
