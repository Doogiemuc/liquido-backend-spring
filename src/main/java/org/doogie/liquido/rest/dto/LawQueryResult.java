package org.doogie.liquido.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.doogie.liquido.rest.LawRestController;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resources;

import java.util.Collection;

/**
 * Result of a /laws/search/findByQuery call that holds
 * the list of matching LawModels as content and
 * the original query parameters of this search.
 *
 * This class holds a list of spring HATOAS Resources
 *
 * @see  LawRestController#searchForLaw(LawQuery)
 * @param <T> one LawModel  (NOT a collection of LawModels!)
 */
public class LawQueryResult<T> extends Resources<T> {

	private LawQuery query;
	private long totalElements;

	public LawQueryResult(Collection<T> content, LawQuery query, long totalElements, Link... links) {
		super(content);
		this.query = query;
		this.totalElements = totalElements;
	}

	//@JsonProperty("query")
	public LawQuery getQuery() {
		return this.query;
	}

	public long getTotalElements() { return this.totalElements; }

	// at least always include the _embedded: {} attribute. But it might still be empty. No empty array as with @RepositoryRestResource
	// https://stackoverflow.com/questions/30286795/how-to-force-spring-hateoas-resources-to-render-an-empty-embedded-array
	@JsonInclude
	@Override
	public Collection<T> getContent() {
		return super.getContent();
	}

}
