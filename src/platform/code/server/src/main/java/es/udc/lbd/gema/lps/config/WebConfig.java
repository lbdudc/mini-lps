package es.udc.lbd.gema.lps.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
/*% if (feature.T_FileUploader || feature.MV_MS_GJ_Cached) { %*/
import org.springframework.scheduling.annotation.EnableScheduling;
/*% } %*/
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
/*% if (feature.T_Swagger) { %*/
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
/*% } %*/
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;

@Configuration
/*% if (!feature.MV_MS_GJ_Cached) { %*/
@EnableWebMvc
/*% } %*/
/*% if (feature.T_FileUploader || feature.MV_MS_GJ_Cached) { %*/
@EnableScheduling
/*% } %*/
public class WebConfig implements WebMvcConfigurer {

  @Inject
  private Properties properties;

  /**
   * CORS: the configured client host, and the site the request itself was made to (whatever the
   * scheme). The app is served by nginx next to this API, so a browser at https://<domain> is the
   * same origin as the API; the scheme is not taken from the proxy's X-Forwarded-Proto because a
   * TLS-terminating front (a tunnel, a load balancer) may not send it, and the server would then
   * take its own site for a foreign origin and answer 403 to every change.
   */
  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilter() {
    CorsConfigurationSource source = request -> {
      List<String> origins = new ArrayList<>();
      origins.add(properties.getClientHost());
      String host = request.getHeader("Host");
      if (host != null && !host.isBlank()) {
        origins.add("http://" + host);
        origins.add("https://" + host);
      }
      CorsConfiguration config = new CorsConfiguration();
      config.setAllowedOrigins(origins);
      config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
      config.addAllowedHeader("*");
      config.setMaxAge(1800L);
      return config;
    };
    FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
  /*% if (feature.T_Swagger) { %*/
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
      .addResourceHandler("swagger-ui.html")
      .addResourceLocations("classpath:/META-INF/resources/");

    registry
      .addResourceHandler("/webjars/**")
      .addResourceLocations("classpath:/META-INF/resources/webjars/");
  }
  /*% } %*/
  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().defaultViewInclusion(true).build();
    // remove default converter to override it with custom one
    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
    // add custom converter
    converters.add(new MappingJackson2HttpMessageConverter(mapper));
  }
}
