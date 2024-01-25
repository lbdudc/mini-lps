/*% if (feature.T_FileUploader) { %*/
package es.udc.lbd.gema.lps.component.file_uploader.file_uploaders;

import org.springframework.stereotype.Component;

@Component
public class FileUploadGeneric extends FileUpload {

    private static final String FOLDER = "";

    protected String getFolder() {
        return FOLDER;
    }

}
/*% } %*/
