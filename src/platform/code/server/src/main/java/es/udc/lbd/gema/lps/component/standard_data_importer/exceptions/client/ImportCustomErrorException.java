/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;

import org.springframework.http.HttpStatus;

public class ImportCustomErrorException extends ImportException {

    public ImportCustomErrorException(String message) {
        super(message, "", HttpStatus.BAD_REQUEST);
        logger.warn("Custom error: " + message);
    }
}
/*% } %*/
