/*% if (feature.MV_T_E_F_URL) { %*/
package es.udc.lbd.gema.lps.component.map_export_management;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import java.util.Random;
import java.lang.StringBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.udc.lbd.gema.lps.component.map_export_management.model.domain.ExportedConfigEntity;
import es.udc.lbd.gema.lps.component.map_export_management.ExportedConfigService;

import org.json.simple.JSONObject;

@RestController
@RequestMapping("/api/export-management")
public class ExportConfigResource {

    private static final Logger log = LoggerFactory.getLogger(ExportConfigResource.class);

    @Inject
    private ExportedConfigService exportedConfigService;

    /**
     * POST  /export : Save the exported data and create a token associated.
     *
     * @param data
     * @return the ResponseEntity with status 201 (Created) and the token associated. If there's some error
     * returns the ResponseEntity with status 500 (Internal Server Error) and the catched error.
     */
    @PostMapping(path = "/export")
    public ResponseEntity<?> saveExportedData(@RequestBody String json) {
        log.debug("POST request to save the current state of the map");
        String token = generateToken();

        try {
            ExportedConfigEntity exportedConfig = exportedConfigService.createExportedConfig(token, json);
        } catch(Exception e) {
            return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        JSONObject result = new JSONObject();
        result.put("token", token);

        return new ResponseEntity<>(result.toString(), HttpStatus.CREATED);
    }

    /**
     * GET  /import/{key} : Get all the data exported by a given token.
     *
     * @param identifier
     * @return the ResponseEntity with status 200 (Ok) and the JSON asociated to the given token. If any data was
     * found, return the ResponseEntity with status 404 (Not Found).
     */
    @GetMapping(path = "/import/{token}")
    public ResponseEntity<?> getExportedData(@PathVariable String token) {
        log.debug("GET request to get the exported state of the map from a given identifier");

        String json = exportedConfigService.getJSONFromToken(token);

        if (json == null){
            String response = "Token " + token + " not found.";
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    /**
     * Set of characters used to generate the key
    */
    private static char[] chars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'q', 'w', 'e', 'r', 't', 'z', 'u', 'i', 'o', 'p', 'a', 's',
        'd', 'f', 'g', 'h', 'j', 'k', 'l', 'y', 'x', 'c', 'v', 'b', 'n', 'm', 'Q', 'W', 'E', 'R', 'T', 'Z', 'U', 'I', 'O', 'P', 'A',
        'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Y', 'X', 'C', 'V', 'B', 'N', 'M' };

    /**
     * Function that generates a random key with length 7
    */
    private static String generateToken() {
        StringBuilder stringBuilder = new StringBuilder();
        int length = 7;

        for (int i = 0; i < length; i++) {
            stringBuilder.append(chars[new Random().nextInt(chars.length)]);
        }
        return stringBuilder.toString();
    }

}

/*% } %*/
