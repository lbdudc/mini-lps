/* Branding (basicData.extra: app_title, primary_color, logo) -- see gispublisher's options-util.js */
const LOGO_FILE = /*%= JSON.stringify(getExtraConfigFromSpec(data, "logo", "")) %*/;

export default {
  /*% if (feature.MV_MS_GeoServer) { %*/GEOSERVER_URL: process.env.VUE_APP_GEOSERVER_URL,/*% } %*/
  /*% if (feature.MV_Processes) { %*/QGIS_URL: process.env.VUE_APP_QGIS_URL,/*% } %*/
  SERVER_URL: process.env.VUE_APP_SERVER_URL,
  APP_NAME: /*%= JSON.stringify(appTitle(data)) %*/,
  /*% if (feature.MV_T_F_Geocoder) { %*/GEOCODER_URL: /*%= JSON.stringify(getExtraConfigFromSpec(data, "geocoder_url", "https://nominatim.openstreetmap.org/search")) %*/,/*% } %*/
  PRIMARY_COLOR: /*%= JSON.stringify(getExtraConfigFromSpec(data, "primary_color", "")) %*/,
  LOGO: LOGO_FILE ? process.env.BASE_URL + LOGO_FILE : "",
}
