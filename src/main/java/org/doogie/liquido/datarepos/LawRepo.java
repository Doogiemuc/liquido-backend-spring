package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Database abstraction for MongoDB collection "laws".
 */
public interface LawRepo extends MongoRepository<LawModel, String> {

}
