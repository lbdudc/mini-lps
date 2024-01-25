/*% if (feature.DM_DI_DataFeeding) { %*/
package es.udc.lbd.gema.lps.component.standard_data_importer.custom;

public class ImportColumnJSON {
    public static final String CUSTOM = "custom";

    private String name;
    private String type;
    private String simpleType;
    private String pattern;
    private String patternCustom; // Used if pattern = 'custom'

    public ImportColumnJSON() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSimpleType() {
        return simpleType;
    }

    public void setSimpleType(String simpleType) {
        this.simpleType = simpleType;
    }

    @Override
    public String toString() {
        return "ImportColumnJSON [name=" + name + ", type=" + type + ", simpleType=" + simpleType + "]";
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPatternCustom() {
        return patternCustom;
    }

    public void setPatternCustom(String patternCustom) {
        this.patternCustom = patternCustom;
    }

    /**
     * If 'custom pattern' is provided returns custom pattern, otherwise returns pattern
     */
    public String getRealPattern() {
        return this.pattern.equals(CUSTOM) ? this.patternCustom : this.pattern;
    }
}
/*% } %*/
