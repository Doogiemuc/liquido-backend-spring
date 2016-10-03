package org.doogie.liquido.datarepos;

import org.bson.types.ObjectId;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Database abstraction for MongoDB collection "delegations".
 */
public interface DelegationRepo extends MongoRepository<DelegationModel, String>, DelegationRepoCustom {

  /**
   * find delegations to a given proxy in the given area
   * @param to ObjectId of proxy user
   * @param area ObjectId of area
   * @return all <b>direct</b> delegations to this proxy in that area
   */
  List<DelegationModel> findByToAndArea(ObjectId to, ObjectId area);

}
