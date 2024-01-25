/*% if (feature.T_EntitiesInformation) { %*/
package es.udc.lbd.gema.lps.component.entities_information;

import jakarta.inject.Inject;
import java.util.Collection;
import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import es.udc.lbd.gema.lps.component.entities_information.custom.EntityJSON;
import es.udc.lbd.gema.lps.component.entities_information.custom.EntityPropertyJSON;

@RestController
@RequestMapping(EntityRestController.ENTITIES_REST_URL)
public class EntityRestController {
    public final static String ENTITIES_REST_URL = "/api/entities";

    @Inject
    EntityService entityService;

    /**
     * Obtains all entities
     */
    @RequestMapping(method = RequestMethod.GET)
    public Collection<EntityJSON> getEntities() {
        return entityService.getEntities();
    }

    /**
     * Obtains all entities simple names
     */
    @RequestMapping(value = "/names", method = RequestMethod.GET)
    public List<String> getEntitiesNames() {
        return entityService.getEntitiesNames();
    }

    /**
     * Obtains details for an entity
     */
    @RequestMapping(value = "/{entityName}", method = RequestMethod.GET)
    public EntityJSON getEntity(@PathVariable(value = "entityName") String entityName) {
        return entityService.getEntity(entityName);
    }

    /**
     * Obtains all properties for the specified entity
     */
    @RequestMapping(value = "/{entityName}/properties", method = RequestMethod.GET)
    public Collection<EntityPropertyJSON> getProperties(@PathVariable(value = "entityName") String entityName) {
        return entityService.getProperties(entityName);
    }

    /**
     * Obtains the specified property for the specified entity
     */
    @RequestMapping(value = "/{entityName}/properties/{propertyName}", method = RequestMethod.GET)
    public EntityPropertyJSON getProperty(@PathVariable(value = "entityName") String entityName,
            @PathVariable(value = "propertyName") String propertyName) {
        return entityService.getProperty(entityName, propertyName);
    }
}
/*% } %*/
