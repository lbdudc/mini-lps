/*% if (feature.T_EntitiesInformation) { %*/
package es.udc.lbd.gema.lps.component.entities_information.custom;

import java.util.Set;

public class EntityJSON {

    private String name;
    private String simpleName;
    private Set<EntityPropertyJSON> properties;

    public EntityJSON() {
    }

    public EntityJSON(String name, String simpleName, Set<EntityPropertyJSON> properties) {
        super();
        this.name = name;
        this.simpleName = simpleName;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public Set<EntityPropertyJSON> getProperties() {
        return properties;
    }

    public void setProperties(Set<EntityPropertyJSON> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "EntityJSON [name=" + name + ", simpleName=" + simpleName + ", properties=" + properties + "]";
    }
}
/*% } %*/
