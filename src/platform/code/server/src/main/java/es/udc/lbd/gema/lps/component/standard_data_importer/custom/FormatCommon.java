/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.custom;

import java.io.File;
import org.apache.commons.io.FilenameUtils;

public abstract class FormatCommon {
    protected File file;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * @return File extension
     */
    public String getExtension() {
        return FilenameUtils.getExtension(file.getName());
    }
}
/*% } %*/
