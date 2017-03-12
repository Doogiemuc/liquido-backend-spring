package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Database abstraction for "ballots".
 *
 * All writing methods are not exposed as rest endpoints!
 * Posting a ballot (ie. cast a vote) is handled in our custom {@link org.doogie.liquido.rest.BallotRestController}
 */
@RepositoryRestResource(collectionResourceRel = "ballots", path = "ballots", itemResourceRel = "ballot")
public interface BallotRepo extends CrudRepository<BallotModel, Long> {
  //TODO: do not expose Ballots as rest resource at all. Clients must use /postBallot in BallotRestController instead

  @Override
  @RestResource(exported = false)
  BallotModel save(BallotModel ballot);

  @RestResource(exported = false)
  void delete(Long id);

  @Override
  @RestResource(exported = false)
  void delete(BallotModel ballotModel);

  @Override
  @RestResource(exported = false)
  void delete(Iterable<? extends BallotModel> iterable);

  @Override
  @RestResource(exported = false)
  void deleteAll();

  /**
   * This can be used to check whether a user has already voted.
   * An initialProposal and a voterToken uniquely identify one ballot!
   * @param initialProposal an initial proposal
   * @param voterToken bcrypt voter token hash value
   * @return the ballot for this user's vote
   */
  BallotModel findByInitialProposalAndVoterToken(LawModel initialProposal, String voterToken);
}
