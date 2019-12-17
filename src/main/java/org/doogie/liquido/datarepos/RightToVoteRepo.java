package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.RightToVoteModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.CastVoteService;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for valid checksums of voterTokens. Here the rightToVote = hash(voterToken) for valid voterTokens is stored.
 * Keep in mind that its mathematically not possible to calculate the voterToken from a rightToVote value.
 * This repo is NOT exposed via REST.
 */
public interface RightToVoteRepo extends CrudRepository<RightToVoteModel, Long> {

	/**
	 * Find a given checksumModel.
	 * @param hashedVoterToken a hashed voterToken
	 * @return the model, or null if not found
	 */
	Optional<RightToVoteModel> findByHashedVoterToken(String hashedVoterToken);

	/**
	 * Find all rightToVotes that are delegated to a given proxy.
	 * @param proxiesRightToVote RightToVoteModel of a proxy
	 * @return all RightToVoteModels that are delegated to this proxy. Transitive and non transitive ones.
	 */
	List<RightToVoteModel> findByDelegatedTo(RightToVoteModel proxiesRightToVote);

	/**
	 * find the checksum of a public proxy so that a voter can delegate his checksum to it.
	 * @pararm area area of the checksum and public proxy
	 * @param proxy a public proxy that added his username to his stored checksum {@link org.doogie.liquido.services.ProxyService#becomePublicProxy(UserModel, AreaModel, String)}
	 * @return the checksum of the public proxy  or null if none was found
	 */
	Optional<RightToVoteModel> findByAreaAndPublicProxy(AreaModel area, UserModel proxy);


}
