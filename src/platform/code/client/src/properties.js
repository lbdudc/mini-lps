export default {
  /*% if (feature.MV_MS_GeoServer) { %*/GEOSERVER_URL: process.env.VUE_APP_GEOSERVER_URL,/*% } %*/
  /*% if (feature.MV_Processes) { %*/QGIS_URL: process.env.VUE_APP_QGIS_URL,/*% } %*/
  SERVER_URL: process.env.VUE_APP_SERVER_URL,
  APP_NAME: "/*%= data.basicData.name %*/",
}
