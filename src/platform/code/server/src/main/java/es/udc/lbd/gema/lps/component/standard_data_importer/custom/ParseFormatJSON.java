/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.custom;

import org.apache.commons.lang3.StringUtils;

public class ParseFormatJSON {

    private String entityName;
    private String file;
    private String url;
    private String encoding;
    private boolean skipFirstLine;
    private ImportColumnJSON[] columns;
    private String type;
    private int ncolumns;
    private Boolean storedFile;

    // CSV properties
    private char separator;
    private String quote;

    public ParseFormatJSON() {
        super();
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isSkipFirstLine() {
        return skipFirstLine;
    }

    public void setSkipFirstLine(boolean skipFirstLine) {
        this.skipFirstLine = skipFirstLine;
    }

    public char getSeparator() {
        return separator;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public String getQuote() {
        return quote;
    }

    public void setQuote(String quote) {
        this.quote = quote;
    }

    public ImportColumnJSON[] getColumns() {
        return columns;
    }

    public void setColumns(ImportColumnJSON[] columns) {
        this.columns = columns;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNcolumns() {
        return ncolumns;
    }

    public void setNcolumns(int ncolumns) {
        this.ncolumns = ncolumns;
    }

    public Boolean getStoredFile() {
      return storedFile;
    }

    public void setStoredFile(Boolean storedFile) {
      this.storedFile = storedFile;
    }

    /**
     * @return File extension obtained from file or from url
     */
    public String getExtension() {
        if (file != null) {
            return StringUtils.substringAfterLast(file, ".");
        }
        if (url != null) {
            return StringUtils.substringAfterLast(url, ".");
        }
        return null;
    }

}
/*% } %*/
