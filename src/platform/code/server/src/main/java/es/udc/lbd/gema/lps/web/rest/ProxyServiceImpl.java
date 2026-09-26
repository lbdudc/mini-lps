/*% if (feature.MV_Processes) { %*/
package es.udc.lbd.gema.lps.web.rest;

import es.udc.lbd.gema.lps.model.service.exceptions.RequestNotSuccesfulException;
import es.udc.lbd.gema.lps.web.rest.custom.HTTPRequestDTO;
import java.io.IOException;
import java.net.HttpURLConnection;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ProxyServiceImpl implements ProxyService {

  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 120_000;

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

    // Only the QGIS services of this stack and public servers (see ProxyGuard)
    ProxyGuard.check(httpRequest.getUrl());

    // Create the RestTemplate. A redirect is handed back, not followed: it could lead to an address
    // the guard refuses. Timeouts: a slow server must not hold the proxy for ever.
    SimpleClientHttpRequestFactory factory =
        new SimpleClientHttpRequestFactory() {
          @Override
          protected void prepareConnection(HttpURLConnection connection, String httpMethod)
              throws IOException {
            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(false);
          }
        };
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
    RestTemplate restTemplate = new RestTemplate(factory);

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

      // If expected binary response fails retry as string. What really is binary (an image, a
      // tile) must not be retried: read as a string it would come back corrupted.
      MediaType contentType = response.getHeaders().getContentType();
      if (responseType == byte[].class
          && contentType != null
          && !contentType.includes(MediaType.APPLICATION_OCTET_STREAM)
          && !"image".equals(contentType.getType())) {
        response =
            restTemplate.exchange(
                httpRequest.getUrl().toString(), httpRequest.getMethod(), entity, String.class);
      }

      return new ResponseEntity<>(
          response.getBody(), prepareHeaders(response.getHeaders()), response.getStatusCode());
    } catch (HttpStatusCodeException e) {
      // The remote server answered with an error status (e.g. 404 for an expired job): hand it
      // over as it is, so the client can tell it apart from a server that does not respond.
      return new ResponseEntity<>(
          e.getResponseBodyAsString(),
          e.getResponseHeaders() != null
              ? prepareHeaders(e.getResponseHeaders())
              : new HttpHeaders(),
          e.getStatusCode());
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
        // never the browser's say on credentials or cookies
        if (!ProxyGuard.FORWARDED_HEADERS.contains(header.getKey().toLowerCase())) {
          continue;
        }
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

/*% } %*/
