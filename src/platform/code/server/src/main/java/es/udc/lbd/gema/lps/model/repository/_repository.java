/*%@ return data.dataModel.entities
      .filter(function(context){
        return !context.abstract;
      })
      .map(function(en) {
        return {
            fileName: normalize(en.name, true) + 'Repository.java',
            context: en
        };
      });
%*/
package es.udc.lbd.gema.lps.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import es.udc.lbd.gema.lps.model.domain./*%= normalize(context.name, true) %*/;
import org.springframework.data.repository.query.Param;

/*%
    function camelToSnake(string) {
       return string.replace(/[\w]([A-Z])/g, function(m) {
           return m[0] + "_" + m[1];
       }).toLowerCase();
    }

    var pk = normalize(getEntityProperty(context, 'id').class.split(' ')[0], true);
    var pkName = normalize(getEntityProperty(context, 'id').name);
%*/
public interface /*%= normalize(context.name, true) %*/Repository
  extends JpaRepository</*%= normalize(context.name, true) %*/, /*%= pk %*/>, JpaSpecificationExecutor</*%= normalize(context.name, true) %*/> {

    /*% if (feature.DM_DI_DataFeeding) { %*/

    // TODO needed to recover save operation with reflection (feature standardDataImport)
    /*%= normalize(context.name, true) %*/ save(/*%= normalize(context.name, true) %*/ /*%= normalize(context.name) %*/);
    /*% } %*/

    Optional</*%= normalize(context.name, true) %*/> findById(/*%= pk %*/ pk);

    Page</*%= normalize(context.name, true) %*/> findByIdIn(List</*%= pk %*/> pk, Pageable pageable);

    /*% getOtherRelationshipsNotMultipleAndOwning(data, context).forEach(function(r) { %*/
    @Query(value = "select * from t_/*%= camelToSnake(normalize(context.name)) %*/ a where a./*%= camelToSnake(pkName) %*/ not in (select coalesce(/*%= camelToSnake(normalize(r.property.name)) %*/, -1) from t_/*%= camelToSnake(normalize(r.entity.name)) %*/)", nativeQuery = true)
    List</*%= normalize(context.name, true) %*/> /*%= r.property.bidirectional %*/IsNull();
    /*% }); %*/

    /*% if (feature.MV_MS_GJ_Paginated) { %*/
    /*%
      var geographicPropertyNames = getEntityPropertyNamesOfGeographicTypes(context);
      geographicPropertyNames.forEach(function(geoPropertyName) {
        geoPropertyName = normalize(geoPropertyName);
    %*/
    @Query(
      value =
        "select * from t_/*%= camelToSnake(normalize(context.name)) %*/ a where ST_Intersects(a./*%= geoPropertyName %*/, ST_MakeEnvelope(:xmin, :ymin, :xmax, :ymax, /*%= data.basicData.SRID || 4326  %*/))",
      nativeQuery = true)
    List</*%= normalize(context.name, true) %*/> getDataByBoundingBox(
      @Param("xmin") Double xmin,
      @Param("xmax") Double xmax,
      @Param("ymin") Double ymin,
      @Param("ymax") Double ymax);
    /*% }); %*/
    /*% } %*/
}
