/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Created by sfernandez on 30/11/2016.
 */
public class UploadFileNotProvidedException extends UploadException {

    public UploadFileNotProvidedException() {
        super("upload.file_required", HttpStatus.BAD_REQUEST);
        logger.warn("Upload: File not provided.");
    }

}
/*% } %*/
