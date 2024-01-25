/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;

import org.springframework.http.HttpStatus;

public class ImportValueNotValidException extends ImportException {

    public ImportValueNotValidException(String param) {
        super("import.value_format_exception", param, HttpStatus.BAD_REQUEST);
        logger.warn("Import: Value '{}' is not valid.", param);
    }
}
/*% } %*/
