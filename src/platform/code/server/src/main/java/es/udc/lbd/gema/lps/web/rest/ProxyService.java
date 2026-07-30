/*% if (feature.MV_Processes) { %*/
package es.udc.lbd.gema.lps.web.rest;

import es.udc.lbd.gema.lps.model.service.exceptions.RequestNotSuccesfulException;
import es.udc.lbd.gema.lps.web.rest.custom.HTTPRequestDTO;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

public interface ProxyService {

  public ResponseEntity<?> sendRequest(HTTPRequestDTO httpRequest)
      throws RestClientException,
          URISyntaxException,
          RequestNotSuccesfulException,
          MalformedURLException;
}

/*% } %*/
