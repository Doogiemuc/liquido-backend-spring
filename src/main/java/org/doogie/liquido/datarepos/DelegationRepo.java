package org.doogie.liquido.datarepos;

import org.bson.types.ObjectId;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

/**
 * Database abstraction for MongoDB collection "delegations".
 */
@RepositoryRestResource(collectionResourceRel = "delegations", path = "delegations")
public interface DelegationRepo extends MongoRepository<DelegationModel, String>, DelegationRepoCustom {

  //Delegations have a combined unique index on area,fromUSer and toProxy
  //db.delegations.createIndex({ "area":1, "fromUser":1, "toProxy":1 }, { unique: true })
  //
  //And their "foreign keys", the ObjectIDs must actually exist in the referenced user and area collections
  //which is checked by DelegationValidator.java

  /**
   * find delegations to a given proxy in the given area
   * @param toProxy ObjectId of proxy user
   * @param area ObjectId of area
   * @return all <b>direct</b> delegations to this proxy in that area
   */
  @RestResource(exported = false)
  List<DelegationModel> findByToProxyAndArea(ObjectId toProxy, ObjectId area);

  /**
   * find delegations to a given proxy in the given area
   * @param toProxyId ID of a proxy user
   * @param areaId ID of an area
   * @return all <b>direct</b> delegations to this proxy in that area as a List of DelegationModels (may be empty list)
   */
  List<DelegationModel> findByToProxyAndArea(String toProxyId, String areaId);

}
