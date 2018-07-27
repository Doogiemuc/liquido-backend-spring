package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.PollModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Database abstraction for "ballots".
 *
 * Ballots are not exposed as RepositoryRestResource at all.
 * Posting a ballot (ie. cast a vote) is handled in our custom {@link org.doogie.liquido.rest.BallotRestController}
 */
//Do not expose Ballots as rest resource at all. Clients must use /postBallot in BallotRestController instead
//@RepositoryRestResource(collectionResourceRel = "ballots", path = "ballots", itemResourceRel = "ballot")
public interface BallotRepo extends CrudRepository<BallotModel, Long> {

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
   * Find the ballot with that ballotToken in that poll
   * @param poll a PollModel
   * @param areaToken bcrypt voter token hash value
   * @return the ballot for this (still anonymous) user's vote
   */
  BallotModel findByPollAndAreaToken(PollModel poll, String areaToken);
}
