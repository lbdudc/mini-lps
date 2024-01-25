/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportDateUnparseableException extends ImportException {

    public ImportDateUnparseableException(String param) {
        super("import.date_cannot_be_parsed", param, HttpStatus.BAD_REQUEST);
        logger.warn("Import: Date '{}' cannot be parsed.", param);
    }
}
/*% } %*/
