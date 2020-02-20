package org.doogie.liquido.rest.dto;

import lombok.Data;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.rest.LawRestController;

import java.util.List;

/**
 * Result of a /laws/search/findByQuery call that holds
 * the list of matching LawModels as content and
 * the original query parameters of this search.
 *
 * This class holds a list of spring HATOAS Resources
 *
 * @see  LawRestController#searchForLaw(LawQuery)
 */
@Data
public class LawQueryResult {
	private LawQuery query;
	private List<LawModel> laws;
	private long totalElements;
}
