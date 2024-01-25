/*% if (feature.T_EntitiesInformation) { %*/
package es.udc.lbd.gema.lps.component.entities_information;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityInfo {
    /**
     * Indicates whether the feature will be displayed on forms and other elements
     */
    boolean hidden() default false;

    /**
     * Indicates which roles can view the entity or the property
     */
    String[] access() default {};
}
/*% } %*/
