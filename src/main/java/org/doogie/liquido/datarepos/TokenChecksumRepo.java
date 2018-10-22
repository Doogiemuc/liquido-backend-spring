package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.TokenChecksumModel;
import org.doogie.liquido.model.UserModel;
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
	List<TokenChecksumModel> findByDelegatedTo(TokenChecksumModel proxyChecksum);

	/**  DEPRECATED
	 * find the checksum of a public proxy so that a voter can delegate his checksum to it.
	 * @pararm area area of the checksum and public proxy
	 * @param proxy a public proxy that added his uername to his stored checksum
	 * @return the checksum of the public proxy  or null if none was found
	 *
	TokenChecksumModel findByAreaAndPublicProxy(AreaModel area, UserModel proxy);
	*/
}
