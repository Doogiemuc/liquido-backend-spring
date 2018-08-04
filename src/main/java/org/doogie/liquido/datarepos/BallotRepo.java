package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.rest.VoteRestController;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Database abstraction for "ballots".
 *
 * Ballots are not exposed as RepositoryRestResource at all.
 * Posting a ballot (ie. cast a vote) is handled in our custom {@link VoteRestController}
 */
//Do not expose Ballots as rest resource at all. Clients must use /postBallot in VoteRestController instead
//@RepositoryRestResource(collectionResourceRel = "ballots", path = "ballots", itemResourceRel = "ballot")
public interface BallotRepo extends CrudRepository<BallotModel, Long> {

  /*  not necessary anymore since we are not exporting this as a rest resource at all
      This would be how to hide specific methods in external REST API
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

  */

  /**
   * Find a ballot for a poll with a given checksum, ie. that was casted from a voter
	 * who's secret voterToken hashes to this checksum.
   * @param poll a PollModel
   * @param checksum hash(voterToken)
   * @return the ballot for this (still anonymous) user's vote
   */
  BallotModel findByPollAndChecksum(PollModel poll, String checksum);
}
