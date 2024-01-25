/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.file_uploader.file_uploaders;

import org.springframework.stereotype.Component;

@Component
public class FileUploadImport extends FileUpload {

    private static final String FOLDER = "import";

    protected String getFolder() {
        return FOLDER;
    }

}
/*% } %*/
