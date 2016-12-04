package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Database abstraction for MongoDB collection "users".
 *
 * This Repository is exposed as REST service under the endpint <pre>/users</pre>
 * see https://spring.io/guides/gs/accessing-mongodb-data-rest/
 */
@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepo extends MongoRepository<UserModel, String> {

  /**
   * Find a specific user by email address.
   *
   * <pre>http://localhost:8080/users/search/findByEmail?email=testuser1@liquido.de</pre>
   *
   * @param email users email
   * @return one UserModel or null if no user with that email was found.
   */
  UserModel findByEmail(@Param("email") String email);   // This magically creates a mongo query just from the method name!



}
