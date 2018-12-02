package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.UserModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Database abstraction for "users".
 *
 * This Repository is exposed as REST service under the endpoint <pre>/liquido/v2/users</pre>
 * see https://spring.io/guides/gs/accessing-mongodb-data-rest/
 */
//@CrossOrigin(origins = "*")
//TODO: @PreAuthorize("hasRole('ROLE_VOTER')")   TODO: externalize string ROLE_VOTER to central place
@RepositoryRestResource(collectionResourceRel = "users", path = "users", itemResourceRel = "user")
public interface UserRepo extends CrudRepository<UserModel, Long> {

  /**
   * Find a specific user by email address.
   *
   * <pre>http://localhost:8080/users/search/findByEmail?email=testuser1@liquido.de</pre>
   *
   * @param email users email
   * @return one UserModel or null if no user with that email was found.
   */
  UserModel findByEmail(@Param("email") String email);   // This magically creates a query just from the method name!

  /** find a user by his mobile phone number */
  UserModel findByProfileMobilephone(String mobilephone);

}
