package org.doogie.liquido.rest.converters;

import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.lang.reflect.Type;

/**
 * This converter can deserialize from HATEOAS URIs to spring data entities.
 * It is added as a converter in LiquidoRepositoryRestConfigurer, so that @RequestParams  request URL parameters
 * can be deserialized from an URI (e.g. /entity/4711)  to their corresponding spring data jpa entity.
 * Spring data rest would be able to do this for plain IDs (myentityid=4711) but not for URIs.
 * @see org.springframework.data.rest.core.UriToEntityConverter
 */
public class LiquidoUriToEntityConverter<T> implements Converter<String, T>, ConditionalConverter {

	Type clazz;
	CrudRepository<T, Long> repo;
	String pathSegment;

	public LiquidoUriToEntityConverter(CrudRepository<T, Long> rep, Type clazz) {
		this.repo = rep;
		this.clazz = clazz;
		this.pathSegment = repo.getClass().getInterfaces()[0].getAnnotation(RepositoryRestResource.class).path();
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.getType().equals(String.class) && targetType.getType().equals(clazz)) {
			return true;
		}
		return false;
	}


	@Override
	public T convert(String sourceUri) {
		Long id = LiquidoRestUtils.getIdFromURI(this.pathSegment, sourceUri);
		T entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Cannot find entity "+clazz.getTypeName()+" with id="+id));
		return entity;
	}

}
