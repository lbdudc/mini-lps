/*% if (feature.MV_T_E_F_URL) { %*/
import { HTTP } from "@/common/http-common";
import Logger from "js-logger";

const logger = Logger.get("logger");
const RESOURCE_NAME = "export-management";

export default {
  async export(json) {
    try {
      return (await HTTP.post(`${RESOURCE_NAME}/export`, json, {
        headers: {
          "Content-Type": "application/text"
        }
      })).data;
    } catch (err) {
      logger.error("Error exporting", json);
      throw err;
    }
  },

  async import(token) {
    try {
      return (await HTTP.get(`${RESOURCE_NAME}/import/${token}`)).data;
    } catch (err) {
      logger.error("Error importing", token);
      throw err;
    }
  },
}
/*% } %*/
