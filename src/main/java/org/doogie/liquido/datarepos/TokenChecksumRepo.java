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
	 * Find all checksums that are delegated to this proxy.
	 * @param proxyChecksum checksum of a proxy
	 * @return all checksums that are delegated to this proxy. Transitive and non transitive ones.
	 */
	List<TokenChecksumModel> findByDelegatedTo(TokenChecksumModel proxyChecksum);

	/**
	 * Find checksums that are delegated to this proxy with a check for (non-)transitive delegations
	 * @param proxyChecksum checksum of a proxy
	 * @param transitive whether to check for transitive or non-transitive delegations only
	 * @return all checksums that are delegated to this proxy either transitively or non-transitively
	 */
	List<TokenChecksumModel> findByDelegatedToAndTransitive(TokenChecksumModel proxyChecksum, boolean transitive);

	/**
	 * find the checksum of a public proxy so that a voter can delegate his checksum to it.
	 * @pararm area area of the checksum and public proxy
	 * @param proxy a public proxy that added his username to his stored checksum   {@link org.doogie.liquido.services.ProxyService#becomePublicProxy(UserModel, AreaModel, String)}
	 * @return the checksum of the public proxy  or null if none was found
	 */
	TokenChecksumModel findByAreaAndPublicProxy(AreaModel area, UserModel proxy);

	/**
	 * Count number of pending delegation requests.
	 * @param proxy the proxy that receives the delegations
	 * @return number of pending delegation requests
	 */
	long countByRequestedDelegationTo(UserModel proxy);
}
