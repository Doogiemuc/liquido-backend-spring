package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.converters.LiquidoUriToEntityConverter;
import org.doogie.liquido.rest.deserializer.LawModelDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.jpa.projection.CollectionAwareProjectionFactory;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
@Slf4j
@Configuration                  // a @Configuration is also a @Component
@EnableJpaAuditing(auditorAwareRef = "liquidoAuditorAware")   // automatic handling of UpdatedAt and CreatedAt
@EnableTransactionManagement    // enable @Transactional annotation
//@EnableSwagger2                 // enable autogenerated API documentation
//@Import({springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration.class})
@ComponentScan("org.doogie.liquido")
public class LiquidoRepositoryRestConfigurer implements RepositoryRestConfigurer {

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
    config.exposeIdsFor(LawModel.class);          // actually LawModel has its own LawProjection which exposes IDs.
    config.exposeIdsFor(PollModel.class);
		config.exposeIdsFor(BallotModel.class);
    config.exposeIdsFor(RightToVoteModel.class);

		//TODO: config.getCorsRegistry().addMapping("*").allowedOrigins("*/*");

  }

	/**
	 * This bean is needed in {@link PollRestController#getOwnBallot(PollModel, String)}   to manually create the projection
	 * https://stackoverflow.com/questions/33288486/using-a-spring-data-rest-projection-as-a-representation-for-a-resource-in-a-cus
	 */
	@Bean public CollectionAwareProjectionFactory projectionFactory() { return new CollectionAwareProjectionFactory(); }


	/* DEPRECATED.  This worked, but now I have my  org.doogie.liquido.rest.deserializer.*  @JsonComponent
	 *
	 * Configure our JSON parse so that it convert from URIs to Entity instances
	 * that are loaded from the DB.
	 * Solution https://stackoverflow.com/questions/37186417/resolving-entity-uri-in-custom-controller-spring-hateoas/52938767#52938767
	 * @param objectMapper jackson JSON parser

	@Override
	public void configureJacksonObjectMapper(ObjectMapper objectMapper) {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(AreaModel.class, new JsonDeserializer<AreaModel>() {
			@Override
			public AreaModel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
				String uri = p.getValueAsString();
				//p.getParsingContext().getCurrentName()+"s"  == "areas" :-)
				Long areaId = LiquidoRestUtils.getIdFromURI("areas", uri);
				AreaModel area = areaRepo.findById(areaId).get();
				return area;
			}
		});
		objectMapper.registerModule(module);
	}
  */



	@Autowired
	AreaRepo areaRepo;

	@Autowired
	UserRepo userRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	PollRepo pollRepo;

	/*

	/**
	 * Register custom deserializer as Jackson 2 module, so that we can
	 * @return Jackson 2 Module as Bean
	 *
	@Bean
	public Module userModelDeserializer() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(UserModel.class, new UserModelDeserializer(userRepo));
		return module;
	}

	/**
	 * Add converters for REST RequestParams that can deserialize from HATEOAS URIs to spring data entities.
	 * This is used when the client passes an URI, e.g. "/laws/4711" in rest URLs as PathParameters.
	 * Spring's own implementation can already convert from numerical ID to Entity, but not from URI like e.g. "/laws/4711" to entity.
	 * @param conversionService springs conversion service, that we'll addConverter() to
   */
	@Override
	public void configureConversionService(ConfigurableConversionService conversionService) {
		conversionService.addConverter(String.class, AreaModel.class, new LiquidoUriToEntityConverter<>(areaRepo, AreaModel.class));
		conversionService.addConverter(String.class, LawModel.class,  new LiquidoUriToEntityConverter<>(lawRepo,  LawModel.class));
		conversionService.addConverter(String.class, PollModel.class, new LiquidoUriToEntityConverter<>(pollRepo, PollModel.class));
		conversionService.addConverter(String.class, UserModel.class, new LiquidoUriToEntityConverter<>(userRepo, UserModel.class));
	}

	/*
   * Automatically generated Swagger REST API documentation
   * @return the configured docket bean
  @Bean
  public Docket getSwaggerApi() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(PathSelectors.any())
        .build();
    //.pathMapping("/liquido");
  }
  */



}