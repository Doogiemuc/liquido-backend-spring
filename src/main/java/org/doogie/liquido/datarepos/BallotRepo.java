package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Database abstraction for "ballots".
 */
//TODO: NOT exported as @RepositoryRestResource ! Ballots can only be accessed via BallotRestController
@RepositoryRestResource(collectionResourceRel = "ballots", path = "ballots", itemResourceRel = "ballot")
public interface BallotRepo extends CrudRepository<BallotModel, Long> {

  /**
   * Do not export the save method as rest endpoint that client can POST to.
   * This is handled in our custom {@link org.doogie.liquido.rest.BallotRestController}
   * @param ballot the ballot to save
   * @return the saved ballot incl. ID
   */
  @Override
  @RestResource(exported = false)
  BallotModel save(BallotModel ballot);
}
