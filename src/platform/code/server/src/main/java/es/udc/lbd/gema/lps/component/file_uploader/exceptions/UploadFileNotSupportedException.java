/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class UploadFileNotSupportedException extends UploadException {

    public UploadFileNotSupportedException(String param) {
        super("upload.invalid_extension", param, HttpStatus.BAD_REQUEST);
        logger.warn("Upload: Format not supported '{}'.", param);
    }

}
/*% } %*/
