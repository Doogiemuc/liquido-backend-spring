package org.doogie.liquido.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.ObjectId;
import org.doogie.liquido.datarepos.DelegationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Configure the exposed REST HATEOAS services.
 *
 *  - Configure the base path for our rest endpoints
 *  - add Validators on beforeCreate
 *  - better serialization for MongoDB ObjectIDs  to HEX24 strings
 *
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


  // Do not create an instance with "new". Let Spring inject the dependency, so that it can ba handled by Spring.
  @Autowired
  private DelegationValidator delegationValidator;

  /** add a custom validator for Delegations */
  @Override
  public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
    log.info("========== adding beforeCreate validator     in RepositoryRestConfigurer");
    validatingListener.addValidator("beforeCreate", delegationValidator);
  }

  /*   This does not work. NO idea why not.
  @Bean
  public DelegationValidator beforeCreateDelegationModelValidator() {
    return delegationValidator;
  }
  */

  @Override
  public void configureJacksonObjectMapper(ObjectMapper objectMapper) {
    log.debug("==== configureJacksonObjectMapper");
    SimpleModule myModule = new SimpleModule("ObjectIdJacksonModule");
    myModule.addSerializer(ObjectId.class, new MongoObjectIdSerializer());
    objectMapper.registerModule(myModule);
  }

  private class MongoObjectIdSerializer extends JsonSerializer<ObjectId>{
    // See also this example: https://github.com/jhiemer/spring-data-rest-sample/blob/master/src/main/java/de/cloudscale/config/CustomRepositoryRestMvcConfiguration.java
    //TODO: BUG: This serializer ist not yet picked up in exception handling of spring-data-rest exceptions
    @Override
    public void serialize(ObjectId objectId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
      log.debug("==== serializing ObjectID  from RepositoryRestConfigurer");
      jsonGenerator.writeString(objectId.toHexString());
    }
  }



}