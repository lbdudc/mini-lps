/*% if (feature.MV_T_E_F_URL) { %*/
package es.udc.lbd.gema.lps.component.map_export_management;

import es.udc.lbd.gema.lps.component.map_export_management.model.repository.ExportedConfigRepository;
import es.udc.lbd.gema.lps.component.map_export_management.model.domain.ExportedConfigEntity;

import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing the exported configuration.
 */
@Service
@Transactional
public class ExportedConfigService {

    @Inject
    private ExportedConfigRepository exportedConfigRepository;

    /**
     * Creates a new exported configuration and saves it on database.
     */
    public ExportedConfigEntity createExportedConfig(String token, String json) {

        ExportedConfigEntity newExportedConfig = new ExportedConfigEntity();
        newExportedConfig.setToken(token);
        newExportedConfig.setJson(json);

        exportedConfigRepository.save(newExportedConfig);
        return newExportedConfig;
    }

    /**
     * Returns a JSON with the configuration saved previously with the given key.
     * Returns null if any data is found.
    */
    public String getJSONFromToken(String token) {
        ExportedConfigEntity exportedConfig = exportedConfigRepository.findByToken(token);

        return exportedConfig == null ? null : exportedConfig.getJson();
    }

}

/*% } %*/
