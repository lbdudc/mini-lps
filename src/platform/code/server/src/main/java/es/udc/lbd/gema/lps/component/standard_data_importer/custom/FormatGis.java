/*% if (feature.DM_DI_DF_Shapefile) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.custom;

import org.apache.commons.lang3.StringUtils;

public class FormatGis extends FormatCommon {

    private ImportColumnJSON[] format;
    private String temporaryFileName;
    private String encoding;
    private String entityClazz;

    public String getEntityClazz() {
        return entityClazz;
    }

    public void setEntityClazz(String entityClazz) {
        this.entityClazz = entityClazz;
    }

    public ImportColumnJSON[] getFormat() {
        return format;
    }

    public void setFormat(ImportColumnJSON[] format) {
        this.format = format;
    }

    public String getTemporaryFileName() {
        return temporaryFileName;
    }

    public void setTemporaryFileName(String temporaryFileName) {
        this.temporaryFileName = temporaryFileName;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return File extension obtained from file or from url
     */
    @Override
    public String getExtension() {
        if (temporaryFileName != null) {
            return StringUtils.substringAfterLast(temporaryFileName, ".");
        }
        return null;
    }
}
/*% } %*/
