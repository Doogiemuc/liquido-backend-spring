package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Database abstraction for MongoDB collection "ballot".
 */
//NOT exported as @RepositoryRestResource ! Ballots can only be accessed via BallotRestcontroller
public interface BallotRepo extends MongoRepository<BallotModel, String> {
  // EMPTY
}
