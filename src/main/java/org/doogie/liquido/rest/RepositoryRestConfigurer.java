package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.DelegationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * Configure the exposed REST HATEOAS services.
 *
 *  - Configure that only annotated repositories will be exposed
 *  - Remark: base path for REST endpoint is configured in application.properties
 *  - add Validators on beforeCreate
 *
 * https://spring.io/understanding/HATEOAS
 */
@Configuration       // @Configuration is also a @Component
@EnableJpaAuditing   //(auditorAwareRef = "liquidoAuditorAware")   // this is necessary so that UpdatedAt and CreatedAt are handled.
public class RepositoryRestConfigurer extends RepositoryRestConfigurerAdapter {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Override
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
    log.debug("===== Configuring RepositoryRestconfiguration under basePath='"+config.getBasePath().toString()+"',  baseURI='"+config.getBaseUri().toString()+"'");

    // The base path for RepositoryRestResource is configured in application.properties. It's more prominent there
    // Keep in mind that this is only the base path for our HATEOAS endpoints. The base path for normel @RestControllers has to configured individually there.
    //config.setBasePath("/liquido/v2");

    // Only export data repositories that are annotated with @RepositoryRestResource(...)
    // In future versions this will be configurable in application.properties   spring.data.rest.detection-strategy=visibility   https://github.com/spring-projects/spring-boot/issues/7113
    config.setRepositoryDetectionStrategy(RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED);

    //TODO: when available in future version of spring: config.getCorsRegistry()
  }

  // implementing AuditorAware is not necessary. All already handled by spring boot :-)  https://jaxenter.com/rest-api-spring-java-8-112289.html


  @Autowired        // Do not create an instance with "new". Let Spring inject the dependency, so that it can ba handled by Spring.
  private DelegationValidator delegationValidator;

  /** add a custom validator for Delegations */
  @Override
  public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
    log.info("========== adding beforeCreate validator     in RepositoryRestConfigurer");
    validatingListener.addValidator("beforeCreate", delegationValidator);
  }

}