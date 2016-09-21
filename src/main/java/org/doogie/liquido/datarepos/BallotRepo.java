package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Database abstraction for MongoDB collection "ballot".
 */
public interface BallotRepo extends MongoRepository<BallotModel, String> {

}
