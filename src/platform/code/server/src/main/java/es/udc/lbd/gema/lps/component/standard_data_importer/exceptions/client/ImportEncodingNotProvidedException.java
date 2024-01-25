/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportEncodingNotProvidedException extends ImportException {

    public ImportEncodingNotProvidedException() {
        super("import.encoding_not_provided", HttpStatus.BAD_REQUEST);
        logger.warn("Import: Encoding not provided.");
    }
}
/*% } %*/
