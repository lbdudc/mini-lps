/*% if (feature.DM_DI_DF_Shapefile) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.client;

import es.udc.lbd.gema.lps.component.standard_data_importer.exceptions.ImportException;
import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class ImportGisFileRepeatedException extends ImportException {

    public ImportGisFileRepeatedException(String param) {
        super("import.gis_file_repeated", param, HttpStatus.BAD_REQUEST);
        logger.warn("Import: GIS File '{}' repeated.", param);
    }
}
/*% } %*/
