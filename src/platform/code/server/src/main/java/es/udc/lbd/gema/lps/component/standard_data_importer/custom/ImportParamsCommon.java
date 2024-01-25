/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.custom;

import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

public abstract class ImportParamsCommon {

    protected MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    /**
     * @return File extension
     */
    public String getExtension() {
        return FilenameUtils.getExtension(file.getOriginalFilename());
    }

}
/*% } %*/
