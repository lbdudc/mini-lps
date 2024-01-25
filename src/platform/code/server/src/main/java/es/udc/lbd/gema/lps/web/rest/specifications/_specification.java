/*%@ return data.dataModel.entities.map(function(en) {
    return {
        fileName: normalize(en.name, true) + 'Specification.java',
        context: en
    };
}) %*/
package es.udc.lbd.gema.lps.web.rest.specifications;

import es.udc.lbd.gema.lps.model.domain./*%= normalize(context.name, true) %*/;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.*;
import es.udc.lbd.gema.lps.web.rest.util.specification_utils.SpecificationUtil;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Path;
import org.springframework.web.bind.annotation.*;
public class /*%= normalize(context.name, true) %*/Specification {
    public static Specification</*%= normalize(context.name, true) %*/> searchAll(String search ) {
        return new Specification</*%= normalize(context.name, true) %*/>() {
            @Override
            public Predicate toPredicate(
                  Root</*%= normalize(context.name, true) %*/> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                String stringToFind = ("%" + search + "%").toLowerCase();
                Path path = SpecificationUtil.getPath(root,null);
                /*% context.properties.forEach(function(prop) {
                    var propertyClass = prop.class.split(' ')[0];
                    var propertyIsEntity =
                              data.dataModel.entities
                                  .map(function(en) { return en.name; })
                                  .indexOf(propertyClass) !== -1;
                    if (prop.bidirectional == null && !isGeographicProperty(propertyClass) && propertyClass != 'Date' && propertyClass != 'DateTime' && propertyClass != 'LocalDate') {
                %*/
                predicates.add(
                  criteriaBuilder.like(
                    criteriaBuilder.lower(path.get("/*%= normalize(prop.name) %*/").as(String.class)), stringToFind));
                /*%
                  } else if (!prop.multiple && propertyIsEntity){
                %*/
                predicates.add(
                  criteriaBuilder.like(
                    criteriaBuilder.lower(root.join("/*%= normalize(prop.name) %*/", JoinType.LEFT).get("/*%= getDisplayStringName(getEntity(data, prop.class)) %*/").as(String.class)),
                    stringToFind));
                /*% } }); %*/
                return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
            }
        };
    }
}
