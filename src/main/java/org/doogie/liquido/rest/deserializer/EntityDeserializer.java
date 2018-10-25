package org.doogie.liquido.rest.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.io.IOException;

/**
 * Generic class for deserializing from a String URI to
 * a JPA entity.  First we extract the ID from the URI
 * and then we load the entity via the given CrudRepository.
 *
 * Usage: Extend this class for your Entity, inject the repo in the constructor and annotate your class with @JsonComponent
 * Then spring will automatically detect it and use it when deserializing with Jackson.
 *
 * <pre>
 * @JsonComponent
 * public class MyEntityDeserializer extends EntityDeserializer<MyEntity> {
 * 	@Autowired
 * 	public AreaModelDeserializer(MyRepo myRepo) {
 * 		super(myRepo);
 * 	}
 * }
 * </pre>
 *
 * @param <T> your JPA entity class
 */
public class EntityDeserializer<T> extends StdDeserializer<T> {
	//@Autowired
	//private WebApplicationContext appContext;

	/** JPA repository for this model */
	private CrudRepository<T, Long> repo;
	/** part of the uri before the ID (normally in plural!) e.g.  "users" in  /api/users/4711 */
	private String pathSegment;

	public EntityDeserializer(CrudRepository<T, Long> rep, Class<T> clazz) {
		super(clazz);
		this.repo = rep;
		this.pathSegment = repo.getClass().getInterfaces()[0].getAnnotation(RepositoryRestResource.class).path();
	}

	@Override
	public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		/*  This way one could automatically get the CrudRepository
		Repositories repositories = new Repositories(appContext);
		Object obj = repositories.getRepositoryFor(T.class)  // <=== But java doesn't support reification :-(
				.orElseThrow(() -> new RuntimeException("Cannot find repo for "));
		CrudRepository<T, Long> repo = (CrudRepository)obj;
		*/
		String uri = p.getValueAsString();
		Long id = LiquidoRestUtils.getIdFromURI(this.pathSegment, uri);
		T entity = repo.findById(id)
			.orElseThrow(() -> new RuntimeException("Cannot find entity at uri="+uri));
		return entity;
	}

}
