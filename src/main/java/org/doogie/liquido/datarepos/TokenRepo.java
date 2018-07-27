package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.TokenModel;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for areaTokens.
 * Used in {@link org.doogie.liquido.services.BallotService}
 */
public interface TokenRepo extends CrudRepository<TokenModel, Long> {

	/**
	 * Find a token
	 * @param areaToken
	 * @return
	 */
	TokenModel findByAreaToken(String areaToken);
}
