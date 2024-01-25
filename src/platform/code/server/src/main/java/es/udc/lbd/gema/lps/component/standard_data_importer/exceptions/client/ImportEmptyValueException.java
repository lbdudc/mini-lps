/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

public class ImportEmptyValueException extends ImportException {

    public ImportEmptyValueException(String param) {
        super("import.empty_value_exception", param, HttpStatus.BAD_REQUEST);
        logger.warn("Import: No value received for field '{}' .", param);
    }
}
/*% } %*/
