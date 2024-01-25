/*% if (feature.DM_DI_DF_Shapefile) { %*/
import { HTTP } from "@/common/http-common";

const RESOURCE_NAME = "/import";

export default {
  import(data) {
    return HTTP.put(RESOURCE_NAME, data);
  },
  loadFile(data) {
    return HTTP.post(RESOURCE_NAME, data, {
      headers: {
        "Content-Type": "multipart/form-data"
      }
    });
  }
};
/*% } %*/
