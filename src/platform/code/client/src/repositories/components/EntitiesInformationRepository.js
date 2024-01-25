/*% if (feature.T_EntitiesInformation) { %*/
import { HTTP } from "@/common/http-common";
import Logger from "js-logger";
const logger = Logger.get("logger");
const RESOURCE_NAME = "entities";

export default {
  async getAll() {
    try {
      return (await HTTP.get(RESOURCE_NAME)).data;
    } catch (err) {
      logger.error("Error getting all entities information");
      throw err;
    }
  },
};
/*% } %*/
