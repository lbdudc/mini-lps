/*% if (feature.MV_MS_GeoServer) { %*/
package es.udc.lbd.gema.lps.model.service.util;

import es.udc.lbd.gema.lps.config.GeoServerProperties;
import it.geosolutions.geoserver.rest.HTTPUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Utility class for geoserver operations. */
@Component
public class GeoServerUtil {
    private Logger logger = LoggerFactory.getLogger(GeoServerUtil.class);

    @Inject
    private GeoServerProperties gsProp;

    private GeoServerUtil() {}

    /**
     * Recalculate the native bounding box and the lat/long bounding box for a feature type.
     *
     * @featureType name of the feature type
     */
    public void recalculateFeatureTypeBBox(String featureType) throws IllegalArgumentException {
        if (featureType == null) {
          throw new IllegalArgumentException("Null argument");
        }

        String url =
          gsProp.getUrl()
            + "/rest/workspaces/"
            + gsProp.getWorkspace()
            + "/datastores/"
            + gsProp.getDatastore()
            + "/featuretypes/"
            + featureType
            + "?recalculate=nativebbox,latlonbbox";

        String body = "<featureType><enabled>" + true + "</enabled></featureType>";

        String result = HTTPUtils.put(url, body, "application/xml", gsProp.getUser(), gsProp.getPassword());

        if (result != null) {
            logger.info("Layer bounding box successfully updated (" + gsProp.getWorkspace() + ":" + featureType + ")");
        } else {
            logger.error("Error updating layer bounding box (" + gsProp.getWorkspace() + ":" + featureType + ")");
        }
    }
}
/*% } %*/
