/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportFileInUrlNotExistException extends ImportException {

    public ImportFileInUrlNotExistException() {
        super("import.file_in_url_not_exists", HttpStatus.BAD_REQUEST);
        logger.warn("Import: Provided URL does not contains a file.");
    }
}
/*% } %*/
