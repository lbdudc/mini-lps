/*% if (feature.T_Swagger) { %*/
package es.udc.lbd.gema.lps.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;
import java.util.Map;
import java.util.TreeMap;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI().components(new Components()).info(apiInfo());
  }
  @Bean
  public OpenApiCustomizer sortSchemasAlphabetically() {
    return openApi -> {
      Map<String, Schema> schemas = openApi.getComponents().getSchemas();
      openApi.getComponents().setSchemas(new TreeMap<>(schemas));
    };
  }
  private Info apiInfo() {
    return new Info()
      .title("Sample product")
      .description("API description")
      .license(new License().name("Apache 2.0").url("http://springdoc.org"))
      .version("1.0-SNAPSHOT")
      .contact(new Contact().name("LBD").url("lbd.udc.es").email("alejandro.cortinas@udc.es"));
  }

}
/*% } %*/
