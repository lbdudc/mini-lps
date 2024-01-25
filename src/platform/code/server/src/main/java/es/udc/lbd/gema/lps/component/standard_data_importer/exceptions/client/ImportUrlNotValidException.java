/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportUrlNotValidException extends ImportException {

    public ImportUrlNotValidException() {
        super("import.url_not_valid", HttpStatus.BAD_REQUEST);
        logger.warn("Import: No file or URL provided");
    }
}
/*% } %*/
