/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.exceptions;

import es.udc.lbd.gema.lps.model.service.exceptions.AppException;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public abstract class ImportException extends AppException {

    protected static final Logger logger = LoggerFactory.getLogger(ImportException.class);

    public ImportException(String errorCode, HttpStatus status) {
        super(errorCode, status);
    }

    public ImportException(String errorCode, String parameter, HttpStatus status) {
        super(errorCode, parameter, status);
    }

    public ImportException(String errorCode, Map<String, String> parameters, HttpStatus status) {
        super(errorCode, parameters, status);
    }
}
/*% } %*/
