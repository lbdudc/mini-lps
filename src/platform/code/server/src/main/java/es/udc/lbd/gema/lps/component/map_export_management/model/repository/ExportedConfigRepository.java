/*% if (feature.MV_T_E_F_URL) { %*/
package es.udc.lbd.gema.lps.component.map_export_management.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import es.udc.lbd.gema.lps.component.map_export_management.model.domain.ExportedConfigEntity;

/**
 * Spring Data JPA repository for the ExportedConfig entity.
* */
public interface ExportedConfigRepository extends JpaRepository<ExportedConfigEntity, Long> {

    ExportedConfigEntity findByToken(String token);

}
/*% } %*/
