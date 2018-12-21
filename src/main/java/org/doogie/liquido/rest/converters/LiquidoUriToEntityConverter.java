package org.doogie.liquido.rest.converters;

import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.lang.reflect.Type;

/**
 * This converter can deserialize from IDs <b>and</b> spring-data-HATEOAS URIs to JPA entities.
 * It is added as a converter in {@link org.doogie.liquido.rest.LiquidoRepositoryRestConfigurer}.
 * Now you can use plain numerical IDs (eg. myentity=4711)  or URIs (/basepath/entity/4711) as REST @RequestParams in the request URL.
 *
 * Spring data rest would be able to do this for plain IDs (myentityid=4711) but not for URIs.
 *
 * The other way round from enttity to URI can be done like this:
 * Link areaLink = entityLinks.linkToSingleResource(AreaModel.class, area.getId());
 *
 * @see org.springframework.data.rest.core.UriToEntityConverter
 */
public class LiquidoUriToEntityConverter<T> implements Converter<String, T>, ConditionalConverter {

	Type clazz;
	CrudRepository<T, Long> repo;
	String pathSegment;

	/**
	 * Create a new converter for a domain class.
	 * @param rep the spring-data repository for clazz
	 * @param clazz the class type, e.g. <pre>MyEntity.class</pre>
	 */
	public LiquidoUriToEntityConverter(CrudRepository<T, Long> rep, Type clazz) {
		this.repo = rep;
		this.clazz = clazz;
		this.pathSegment = repo.getClass().getInterfaces()[0].getAnnotation(RepositoryRestResource.class).path();
	}

	/**
	 * Check if this converter can convert from sourceType to targetType.
	 * @param sourceType sourceType.getType() should be String.class   (Spring interstingly also passes String.class when only a numerical ID is given as @RequestParam. It does not pass Integer.class or Long.class)
	 * @param targetType targetType.getType() should match clazz
	 * @return return true if this converter is able to convert
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.getType().equals(String.class) && targetType.getType().equals(clazz)) {
			return true;
		}
		return false;
	}

	/**
	 * Convert from the sourceURI to the entity type T.
	 * @param sourceUri either a numerical ID  or a full URI
	 * @return the entity from the DB if found
	 * @throw IllegalArgumentException When the entity could not be found
	 */
	@Override
	public T convert(String sourceUri) {
		Long id = LiquidoRestUtils.getIdFromURI(this.pathSegment, sourceUri);
		T entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Cannot find entity "+clazz.getTypeName()+" with id="+id));
		return entity;
	}

}
