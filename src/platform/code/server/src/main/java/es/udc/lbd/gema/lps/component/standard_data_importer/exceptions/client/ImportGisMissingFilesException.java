/*% if (feature.DM_DI_DF_Shapefile) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportGisMissingFilesException extends ImportException {

    public ImportGisMissingFilesException() {
        super("import.gis_mandatory_files", HttpStatus.BAD_REQUEST);
        logger.warn("Import: Missing mandatory GIS files.");
    }
}
/*% } %*/
