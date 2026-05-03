package es.udc.lbd.gema.lps.web.rest;

import es.udc.lbd.gema.lps.model.service.exceptions.RequestNotSuccesfulException;
import es.udc.lbd.gema.lps.web.rest.custom.HTTPRequestDTO;
import jakarta.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping(ProxyResource.REQUEST_RESOURCE)
public class ProxyResource {

  public static final String REQUEST_RESOURCE = "/api/proxy";

  @Inject private ProxyService proxyService;

  @PostMapping
  public ResponseEntity<String> handleRequest(
      @RequestBody(required = true) HTTPRequestDTO httpRequest)
      throws RestClientException,
          URISyntaxException,
          RequestNotSuccesfulException,
          MalformedURLException {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Type", "application/xml;charset=UTF-8");

      ResponseEntity<?> response = proxyService.sendRequest(httpRequest);
      response.getHeaders().forEach((key, value) -> headers.set(key, String.join(", ", value)));
      headers.add(
          "Access-Control-Expose-Headers", String.join(", ", response.getHeaders().keySet()));

      return new ResponseEntity<String>(response.getBody().toString(), headers, HttpStatus.OK);
    } catch (RequestNotSuccesfulException e) {
      return new ResponseEntity<String>(HttpStatus.BAD_GATEWAY);
    }
  }

  private void formatQueryParameters(Map<String, String> queryParams) {
    if (queryParams.containsKey("tileSize")) {
      queryParams.replace("tileSize", "\"" + queryParams.get("tileSize") + "\"");
    }
  }

  @PostMapping(path = "/{url}/mapViewer")
  public ResponseEntity<?> mvPostRequest(
      @PathVariable String url,
      @RequestParam Map<String, String> queryParams,
      @RequestBody String body,
      @RequestHeader HttpHeaders headers)
      throws RestClientException,
          URISyntaxException,
          RequestNotSuccesfulException,
          MalformedURLException,
          UnsupportedEncodingException {
    try {
      // Decode the base64-encoded URL
      URL urlFinal =
          new URL(URLDecoder.decode(new String(Base64.getDecoder().decode(url)), "UTF-8"));

      Map<String, String> requestHeaders = new HashMap<>();
      requestHeaders.put("Content-Type", headers.getContentType().toString());

      // Create the HTTP request
      HTTPRequestDTO httpRequest =
          new HTTPRequestDTO(urlFinal, HttpMethod.POST, body, requestHeaders, null);

      ResponseEntity<?> response = proxyService.sendRequest(httpRequest);

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(response.getHeaders().getContentType());
      return new ResponseEntity(response.getBody().toString(), responseHeaders, HttpStatus.OK);
    } catch (RequestNotSuccesfulException e) {
      return new ResponseEntity<String>(HttpStatus.BAD_GATEWAY);
    }
  }

  @GetMapping(path = "/{url}/mapViewer")
  public ResponseEntity<?> mvGetRequest(
      @PathVariable String url,
      @RequestParam Map<String, String> queryParams,
      @RequestHeader HttpHeaders headers)
      throws RestClientException,
          URISyntaxException,
          RequestNotSuccesfulException,
          MalformedURLException,
          UnsupportedEncodingException {
    try {
      // Decode the base64-encoded URL
      URL urlFinal =
          new URL(URLDecoder.decode(new String(Base64.getDecoder().decode(url)), "UTF-8"));

      formatQueryParameters(queryParams);

      // Create the HTTP request
      HTTPRequestDTO httpRequest =
          new HTTPRequestDTO(urlFinal, HttpMethod.GET, "", null, queryParams);

      ResponseEntity<?> response = proxyService.sendRequest(httpRequest);

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(response.getHeaders().getContentType());
      return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);
    } catch (RequestNotSuccesfulException e) {
      return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
    }
  }
}
