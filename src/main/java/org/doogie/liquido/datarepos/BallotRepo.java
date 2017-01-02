package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.springframework.data.repository.CrudRepository;

/**
 * Database abstraction for "ballots".
 */
//NOT exported as @RepositoryRestResource ! Ballots can only be accessed via BallotRestController
public interface BallotRepo extends CrudRepository<BallotModel, Long> {
  // EMPTY
}
