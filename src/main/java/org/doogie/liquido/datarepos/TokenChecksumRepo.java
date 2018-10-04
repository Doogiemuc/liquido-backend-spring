package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.TokenChecksumModel;
import org.doogie.liquido.services.CastVoteService;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for valid voterTokens. Here the checksumModel = hash(voterToken) for all valid voter Tokens is stored.
 * Keep in mind that its mathematically not possible to calculate the voterToken from a checksumModel.
 * Used in {@link CastVoteService}
 */
public interface TokenChecksumRepo extends CrudRepository<TokenChecksumModel, Long> {

	/**
	 * Find a given checksumModel.
	 * @param checksum a hashed voterToken
	 * @return the model, or null if not found
	 */
	TokenChecksumModel findByChecksum(String checksum);

}
