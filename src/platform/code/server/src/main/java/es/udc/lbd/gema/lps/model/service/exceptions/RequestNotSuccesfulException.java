/*% if (feature.MV_Processes) { %*/
package es.udc.lbd.gema.lps.model.service.exceptions;

import org.springframework.http.HttpStatus;

public class RequestNotSuccesfulException extends AppException {

  public RequestNotSuccesfulException(String errorCode) {
    super(errorCode, HttpStatus.SWITCHING_PROTOCOLS);
  }
}

/*% } %*/
