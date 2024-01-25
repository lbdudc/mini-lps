/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportInvalidForeignKeyException extends ImportException {

    public ImportInvalidForeignKeyException(String param) {
        super("import.invalid_fk", param, HttpStatus.BAD_REQUEST);
        logger.warn("Import: Invalid foreign key '{}'.", param);
    }
}
/*% } %*/
