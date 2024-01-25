/*% if (feature.MV_MS_GeoServer) { %*/
    package es.udc.lbd.gema.lps.config;

    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.stereotype.Component;

    @Component
    @ConfigurationProperties(prefix = "geoserver", ignoreUnknownFields = false)
    public class GeoServerProperties {

        private Boolean active;
        private String url;
        private String user;
        private String password;
        private String workspace;
        private String datastore;

        public Boolean getActive() {
          return active;
        }

        public void setActive(Boolean active) {
          this.active = active;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getWorkspace() {
            return workspace;
        }

        public void setWorkspace(String workspace) {
            this.workspace = workspace;
        }

        public String getDatastore() {
            return datastore;
        }

        public void setDatastore(String datastore) {
            this.datastore = datastore;
        }
    }
    /*% } %*/
