package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
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
}
