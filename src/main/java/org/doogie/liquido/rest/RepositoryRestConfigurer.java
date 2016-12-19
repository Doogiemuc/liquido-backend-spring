package org.doogie.liquido.rest;

import org.doogie.liquido.model.BeforeCreateDelegationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.stereotype.Component;

/**
 * Configure the exposed REST HATEOAS services
 * https://spring.io/understanding/HATEOAS
 */
@Component
public class RepositoryRestConfigurer extends RepositoryRestConfigurerAdapter {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java


  @Override
  public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
    log.debug("Configuring RepositoryRestconfiguration under /liquido/v2");

    config.setBasePath("/liquido/v2");

    // Only export data repositories that are annotated with @RepositoryRestResource(...)
    // In future versions this will be configurable in application.properties   spring.data.rest.detection-strategy=visibility   https://github.com/spring-projects/spring-boot/issues/7113
    config.setRepositoryDetectionStrategy(RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED);

  }

  @Override
  public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
    log.info("========== adding beforeSave validator");
    validatingListener.addValidator("beforeCreate", new BeforeCreateDelegationValidator());
  }

  // Overwriting this method is not necessary. MongoMapperModule is automatically recognized by SpringBoot
  // public void configureJacksonObjectMapper(ObjectMapper objectMapper) { ...}
  // See also this example: https://github.com/jhiemer/spring-data-rest-sample/blob/master/src/main/java/de/cloudscale/config/CustomRepositoryRestMvcConfiguration.java
}