/*% if (feature.T_EntitiesInformation) { %*/
package es.udc.lbd.gema.lps.component.entities_information.custom;

import java.util.Map;

public class EntityPropertyConstraintJSON {

    private String annotationType;
    private Map<String,Object> attributes;

    public String getAnnotationType() {
        return annotationType;
    }
    public void setAnnotationType(String annotationType) {
        this.annotationType = annotationType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
/*% } %*/
