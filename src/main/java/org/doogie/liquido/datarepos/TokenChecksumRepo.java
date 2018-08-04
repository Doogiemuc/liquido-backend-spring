package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.TokenChecksumModel;
import org.doogie.liquido.services.CastVoteService;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for areaTokens.
 * Used in {@link CastVoteService}
 */
public interface TokenChecksumRepo extends CrudRepository<TokenChecksumModel, Long> {

	/**
	 * Find a given checksum.
	 * @param checksum a hashed voterToken
	 * @return the model, or null if not found
	 */
	TokenChecksumModel findByChecksum(String checksum);

}
