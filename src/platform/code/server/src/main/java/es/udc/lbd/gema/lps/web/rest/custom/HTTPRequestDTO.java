package es.udc.lbd.gema.lps.web.rest.custom;

import java.net.URL;
import java.util.Map;
import org.springframework.http.HttpMethod;

public class HTTPRequestDTO {
  private URL url;
  private HttpMethod method;
  private String body;
  private Map<String, String> headers;
  private Map<String, String> queryParams;

  // Constructores, getters y setters

  public HTTPRequestDTO() {}

  public HTTPRequestDTO(
      URL url,
      HttpMethod method,
      String body,
      Map<String, String> headers,
      Map<String, String> queryParams) {
    this.url = url;
    this.method = method;
    this.body = body;
    this.headers = headers;
    this.queryParams = queryParams;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public void setMethod(HttpMethod method) {
    this.method = method;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  public void setQueryParams(Map<String, String> queryParams) {
    this.queryParams = queryParams;
  }
}
