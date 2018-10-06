package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.TokenChecksumModel;
import org.doogie.liquido.services.CastVoteService;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

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

	/**
	 * Find checksums of voters that delegated their right to vote to a proxie's checksum
	 * @param proxyChecksum checksum of a proxy
	 * @return all checksums that are delegated to this proxy
	 */
	List<TokenChecksumModel> findByDelegatedTo(String proxyChecksum);

}
