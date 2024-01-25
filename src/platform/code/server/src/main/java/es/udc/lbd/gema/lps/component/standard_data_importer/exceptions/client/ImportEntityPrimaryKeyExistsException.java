/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportEntityPrimaryKeyExistsException extends ImportException {

    public ImportEntityPrimaryKeyExistsException(String param) {
        super("import.entity_pk_already_exists", param, HttpStatus.BAD_REQUEST);
        logger.warn("Import: An entity with primary key '{}' already exists.", param);
    }
}
/*% } %*/
