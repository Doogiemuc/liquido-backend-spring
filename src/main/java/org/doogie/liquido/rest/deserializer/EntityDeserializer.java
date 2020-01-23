package org.doogie.liquido.rest.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.io.IOException;

/**
 * Jackson deserializer for deserializing from a String URI to a JPA entity.
 * This deserializer is for example used, when you POST URIs. These can then automatically be deserialized into
 * the corresponding spring-data-jpa entities.
 *
 * First we extract the ID from the URI and then we load the entity via the given CrudRepository.
 *
 * Usage: Extend this class for your Entity, inject the repo in the constructor and annotate your class with @JsonComponent
 * Then spring will automatically detect it and use it when deserializing with Jackson.
 *
 * <pre>
 * // @JsonComponent   if you add this, then this deserializer becomes the default which can make problems with spring-hateoas internal de/serialization.  It's complicated ....
 * //                  Instead register your deserializer explicitly where necessary. For example in a DTO  annotate an entity attribute with   @JsonDeserialize(using = PollModelDeserializer.class)
 * public class MyEntityDeserializer extends EntityDeserializer<MyEntity> {
 * 	@Autowired  // myRepo can simply be injected
 * 	public AreaModelDeserializer(MyRepo myRepo) {
 * 		super(myRepo, MyEntity.class);
 * 	}
 * }
 * </pre>
 *
 * @param <T> your JPA entity class
 *
 * https://www.baeldung.com/spring-boot-jsoncomponent
 * https://stackoverflow.com/questions/37186417/resolving-entity-uri-in-custom-controller-spring-hateoas
 */
@Slf4j
public class EntityDeserializer<T> extends StdDeserializer<T> {
	//@Autowired
	//private WebApplicationContext appContext;

	/** JPA repository for this model */
	private CrudRepository<T, Long> repo;
	/** part of the uri before the ID (normally in plural!) e.g.  "users" in  /api/users/4711 */
	private String pathSegment;

	//private Class<T> type;

	public EntityDeserializer(CrudRepository<T, Long> rep, Class<T> clazz) {
		super(clazz);
		if (rep == null || clazz == null) throw new IllegalArgumentException("Need rep and clazz!");
		//this.type = clazz;    // Nice hack
		this.repo = rep;
		this.pathSegment = repo.getClass().getInterfaces()[0].getAnnotation(RepositoryRestResource.class).path();
	}

	@Override
	public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		/*  This way one could automatically get the CrudRepository
		Repositories repositories = new Repositories(appContext);
		Object obj = repositories.getRepositoryFor(type)  // <=== Here we need the Class<T> hack
				.orElseThrow(() -> new RuntimeException("Cannot find repo for "));
		CrudRepository<T, Long> repo = (CrudRepository)obj;
		*/
		String uri = p.getValueAsString();
		log.trace("Trying to deserialize a "+this.pathSegment+" from uri="+uri);
		Long id = LiquidoRestUtils.getIdFromURI(this.pathSegment, uri);
		T entity = repo.findById(id).orElseThrow(() -> new RuntimeException("Cannot find entity at uri="+uri));
		return entity;
	}

}
