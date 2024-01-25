package es.udc.lbd.gema.lps.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
/*% if (feature.T_FileUploader || feature.MV_MS_GJ_Cached) { %*/
import org.springframework.scheduling.annotation.EnableScheduling;
/*% } %*/
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedOrigins(properties.getClientHost());
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
