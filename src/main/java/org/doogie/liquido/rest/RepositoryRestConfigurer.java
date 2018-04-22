package org.doogie.liquido.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.doogie.liquido.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Configure the exposed REST HATEOAS services.
 *
 *  - Configure that only annotated repositories will be exposed as REST endpoints
 *  - make Models expose their IDs
 *  - Enable automatic generation of Swagger 2.0 REST API documentation
 *
 * Remark: base path for REST endpoint is configured in application.properties
 * https://spring.io/understanding/HATEOAS
 */
@Configuration       // @Configuration is also a @Component
@EnableJpaAuditing(auditorAwareRef = "liquidoAuditorAware")   // this is necessary so that UpdatedAt and CreatedAt are handled.
@EnableSwagger2
@Import({springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration.class})
//@ComponentScan("org.doogie.liquido.rest")
public class RepositoryRestConfigurer extends RepositoryRestConfigurerAdapter {
  private Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Override
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
    log.debug("Configuring RepositoryRestconfiguration under basePath='"+config.getBasePath().toString()+"',  baseURI='"+config.getBaseUri().toString()+"'");

    // The base path for RepositoryRestResource is configured in application.properties. It's more prominent there
    // Keep in mind that this is only the base path for our HATEOAS endpoints. The base path for normal @RestControllers has to configured individually there.
    //config.setBasePath("/liquido/v2");

    // Only export data repositories that are annotated with @RepositoryRestResource(...)
    // In future versions this will be configurable in application.properties   spring.data.rest.detection-strategy=visibility   https://github.com/spring-projects/spring-boot/issues/7113
    config.setRepositoryDetectionStrategy(RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED);

    config.exposeIdsFor(UserModel.class);
    config.exposeIdsFor(AreaModel.class);
    config.exposeIdsFor(BallotModel.class);
    config.exposeIdsFor(DelegationModel.class);
    config.exposeIdsFor(LawModel.class);          // actually LawModel has its own LawProjection which exposes IDs.
    config.exposeIdsFor(PollModel.class);

    //MAYBE: when available in future version of spring: config.getCorsRegistry()
  }

  // see also LiquidoAuditorAware for handling @CreatedBy   https://jaxenter.com/rest-api-spring-java-8-112289.html

  /*
  @Override
  public void configureConversionService(ConfigurableConversionService conversionService) {
    super.configureConversionService(conversionService);
    conversionService.addConverter();
  }
  */

  /**
   * Automatically generated Swagger REST API documentation
   * @return the configured docket bean
   */
  @Bean
  public Docket getSwaggerApi() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(PathSelectors.any())
        .build();
    //.pathMapping("/liquido");
  }

  /*
   * This is necessary for being able to return plain JSON strings from REST controllers
   * http://stackoverflow.com/questions/15507064/return-literal-json-strings-in-spring-mvc-responsebody
   * @param messageConverters
  @Override
  public void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
    stringConverter.setSupportedMediaTypes(Arrays.asList(
      MediaType.APPLICATION_JSON,
      MediaType.APPLICATION_JSON_UTF8,
      MediaType.TEXT_PLAIN
    ));
    messageConverters.add(stringConverter);
  }
  */




  /*  this manual validator for foreign keys was only necessary with MongoDB. Right now this is handled by MySQL foreign key constraints

  @Autowired        // Do not create an instance with "new". Let Spring inject the dependency, so that it can ba handled by Spring.
  private DelegationValidator delegationValidator;

  @Override
  public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
    log.info("========== adding beforeCreate validator     in RepositoryRestConfigurer");
    validatingListener.addValidator("beforeCreate", delegationValidator);
  }
  */

}