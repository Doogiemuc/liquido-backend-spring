package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Database abstraction for MongoDB collection "users".
 */
public interface UserRepo extends MongoRepository<UserModel, String> {

}
