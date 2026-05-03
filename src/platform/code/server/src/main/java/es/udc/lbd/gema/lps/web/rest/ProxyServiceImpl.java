package es.udc.lbd.gema.lps.web.rest;

import es.udc.lbd.gema.lps.model.service.exceptions.RequestNotSuccesfulException;
import es.udc.lbd.gema.lps.web.rest.custom.HTTPRequestDTO;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ProxyServiceImpl implements ProxyService {

  private static List<String> HOP_BY_HOP_HEADERS =
      Arrays.asList("Connection", "Keep-Alive", "Access-Control-Allow-Origin", "Transfer-Encoding");

  /**
   * POST / : Handles an HTTP request
   *
   * @param httpRequest the HTTP object
   * @return the response of that request
   * @throws URISyntaxException
   * @throws RestClientException
   * @throws MalformedURLException
   * @throws Exception
   */
  @Override
  public ResponseEntity<?> sendRequest(HTTPRequestDTO httpRequest)
      throws RestClientException,
          URISyntaxException,
          RequestNotSuccesfulException,
          MalformedURLException {

    // Create the RestTemplate
    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<String> entity = prepareEntity(httpRequest);
    try {
      // If no content type present in headers default to binary, otherwise string
      MediaType headersContentType = entity.getHeaders().getContentType();
      Class<?> responseType =
          (headersContentType != null
                  && !headersContentType.includes(MediaType.APPLICATION_OCTET_STREAM))
              ? String.class
              : byte[].class;

      ResponseEntity<?> response =
          restTemplate.exchange(
              httpRequest.getUrl().toString(), httpRequest.getMethod(), entity, responseType);

      // If expected binary response fails retry as string
      MediaType contentType = response.getHeaders().getContentType();
      if (responseType == byte[].class
          && !contentType.includes(MediaType.APPLICATION_OCTET_STREAM)) {
        response =
            restTemplate.exchange(
                httpRequest.getUrl().toString(), httpRequest.getMethod(), entity, String.class);
      }

      return new ResponseEntity<>(
          response.getBody(), prepareHeaders(response.getHeaders()), response.getStatusCode());
    } catch (RestClientException e) {
      throw new RequestNotSuccesfulException("El servidor remoto no responde");
    }
  }

  /************************** PRIVATE METHODS ***************************/
  private HttpEntity<String> prepareEntity(HTTPRequestDTO httpRequest)
      throws MalformedURLException {

    // Preparar parámetros de la query, si lo tiene
    if (httpRequest.getQueryParams() != null && httpRequest.getQueryParams().size() >= 1) {
      String url = httpRequest.getUrl().toString() + "?";
      for (String name : httpRequest.getQueryParams().keySet()) {
        String param =
            name.toString() + "=" + httpRequest.getQueryParams().get(name).toString() + "&";
        url = url + param;
      }
      url = url.substring(0, url.length() - 1);
      httpRequest.setUrl(new URL(url));
    }

    // Preparar las cabeceras
    HttpHeaders headers = new HttpHeaders();
    if (httpRequest.getHeaders() != null) {
      for (Map.Entry<String, String> header : httpRequest.getHeaders().entrySet()) {
        if (header.getKey() == "Content-Type") {
          headers.setAccept(Arrays.asList(MediaType.parseMediaType(header.getValue())));
        }
        headers.add(header.getKey(), header.getValue());
      }
    }

    // Enviar la entidad con el cuerpo, si lo tiene
    if (httpRequest.getBody().equals("")) return new HttpEntity<String>("parameters", headers);
    else return new HttpEntity<String>(httpRequest.getBody(), headers);
  }

  private HttpHeaders prepareHeaders(HttpHeaders headers) {
    HttpHeaders newHeaders = new HttpHeaders();

    // don't propagate origin non-forwardable headers
    headers.forEach(
        (key, value) -> {
          if (!HOP_BY_HOP_HEADERS.contains(key)) {
            newHeaders.set(key, String.join(", ", value));
          }
        });

    return newHeaders;
  }
}
