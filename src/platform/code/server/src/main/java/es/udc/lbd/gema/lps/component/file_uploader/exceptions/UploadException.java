/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader.exceptions;

import es.udc.lbd.gema.lps.model.service.exceptions.AppException;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public class UploadException extends AppException {

    protected static final Logger logger = LoggerFactory.getLogger(UploadException.class);

    public UploadException(String errorCode, HttpStatus status) {
        super(errorCode, status);
    }

    public UploadException(String errorCode, String parameter, HttpStatus status) {
        super(errorCode, parameter, status);
    }

    public UploadException(String errorCode, Map<String, String> parameters, HttpStatus status) {
        super(errorCode, parameters, status);
    }
}
/*% } %*/
