/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportColumnDuplicatedException extends ImportException {

    public ImportColumnDuplicatedException(String param) {
        super("import.column_duplicated", param, HttpStatus.BAD_REQUEST);
        logger.warn("Import: Column '{}' is duplicated.", param);
    }
}
/*% } %*/
