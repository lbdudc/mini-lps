/*%@ return data.dataModel.entities.map(function(en) {
  return {
    fileName: normalize(en.name, true) + 'EntityRepository.js',
    context: en
  };
}) %*/
import { HTTP } from "@/common/http-common";
import Logger from "js-logger";

const logger = Logger.get("logger");
/*% if (feature.MV_MS_GJ_Cached) { %*/
const GEOMETRIES_ROUTE = "geometries";
/*% } %*/
const RESOURCE_NAME = "entities//*%= pluralize(normalize(context.name)) %*/";

export default {
  async getAll(options = {}) {
    try {
      return (await HTTP.get(`${RESOURCE_NAME}`, options)).data;
    } catch (err) {
      logger.error("Error fetching all", options);
      throw err;
    }
  },

  async getAllWithout(entityName) {
    try {
      return (await HTTP.get(`${RESOURCE_NAME}/without/${entityName}`)).data;
    } catch (err) {
      logger.error("Error fetching all without " + entityName);
      throw err;
    }
  },

  async get(id) {
    try {
      return (await HTTP.get(`${RESOURCE_NAME}/${id}`)).data;
    } catch (err) {
      logger.error("Error fetching entity with id " + id);
      throw err;
    }
  },

  /*% if (checkEntityContainsGeographicProperties(context)) { %*/
  async getGeom(name/*% if (!feature.MV_MS_GJ_Cached) { %*/, options = {}/*% } %*/) {
    try {
      const response = (

        await HTTP.get(
        /*% if (!feature.MV_MS_GJ_Cached) { %*/
          `${RESOURCE_NAME}/geom/${name}`, options
        /*% } else { %*/
          `${GEOMETRIES_ROUTE}/${name}.json`
        /*% } %*/
        )
      ).data;
      return response || [];
    } catch (err) {
      logger.error("Error getting geometry " + name/*% if (!feature.MV_MS_GJ_Cached) { %*/, options/*% } %*/);
      throw err;
    }
  },
  /*% } %*/

  async save(entity) {
    if (entity.id) {
      try {
        return (await HTTP.put(`${RESOURCE_NAME}/${entity.id}`, entity)).data;
      } catch (err) {
        logger.error("Error updating entity", entity);
        throw err;
      }
    } else {
      try {
        return (await HTTP.post(`${RESOURCE_NAME}`, entity)).data;
      } catch (err) {
        logger.error("Error saving entity", entity);
        throw err;
      }
    }
  },

  async delete(id) {
    try {
      return (await HTTP.delete(`${RESOURCE_NAME}/${id}`))
    } catch (err) {
      logger.error("Error deleting entity with id " + id);
      throw err;
    }
  },
};
