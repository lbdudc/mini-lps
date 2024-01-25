/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportFieldPointPropertiesNotProvidedException extends ImportException {

    public ImportFieldPointPropertiesNotProvidedException() {
        super("import.field_point_required_properties", HttpStatus.BAD_REQUEST);
        logger.warn("Import: Not all required values to create a point provided.");
    }
}
/*% } %*/
