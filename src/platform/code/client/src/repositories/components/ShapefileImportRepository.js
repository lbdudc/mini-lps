/*% if (feature.DM_DI_DF_Shapefile) { %*/
import { HTTP } from "@/common/http-common";
import Logger from "js-logger";

const logger = Logger.get("logger");

const RESOURCE_NAME = "/import";

export default {
  import(data) {
    try {
      return HTTP.put(RESOURCE_NAME, data);
    } catch (err) {
      logger.error("Error importing data", data);
      throw err;
    }
  },

  loadFile(data) {
    try {
      return HTTP.post(RESOURCE_NAME, data, {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      });
    } catch (err) {
      logger.error("Error loading file", data);
      throw err;
    }
  },
};
/*% } %*/
