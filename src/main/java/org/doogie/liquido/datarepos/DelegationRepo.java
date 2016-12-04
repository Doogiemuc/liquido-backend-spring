package org.doogie.liquido.datarepos;

import org.bson.types.ObjectId;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Database abstraction for MongoDB collection "delegations".
 */
@RepositoryRestResource(collectionResourceRel = "delegations", path = "delegations")
public interface DelegationRepo extends MongoRepository<DelegationModel, String>, DelegationRepoCustom {

  /**
   * find delegations to a given proxy in the given area
   * @param toProxy ObjectId of proxy user
   * @param area ObjectId of area
   * @return all <b>direct</b> delegations to this proxy in that area
   */
  List<DelegationModel> findByToProxyAndArea(ObjectId toProxy, ObjectId area);

  /**
   * find delegations to a given proxy in the given area
   * @param toProxyId ID of a proxy user
   * @param areaId ID of an area
   * @return all <b>direct</b> delegations to this proxy in that area as a List of DelegationModels (may be empty list)
   */
  List<DelegationModel> findByToProxyAndArea(String toProxyId, String areaId);

}
