/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.custom;

public class ImportFileJSON {

    private String temporaryFile;
    private String url;
    private String[] values;
    private int ncolumns;

    public ImportFileJSON() {
        super();
    }

    public String getTemporaryFile() {
        return temporaryFile;
    }

    public void setTemporaryFile(String temporaryFile) {
        this.temporaryFile = temporaryFile;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public int getNcolumns() {
        return ncolumns;
    }

    public void setNcolumns(int ncolumns) {
        this.ncolumns = ncolumns;
    }
}
/*% } %*/
