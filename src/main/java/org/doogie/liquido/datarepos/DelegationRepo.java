package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Database abstraction for "delegations".
 *
 * This is a JpaRepository so that I can query by Example.
 */
@RepositoryRestResource(collectionResourceRel = "delegations", path = "delegations", itemResourceRel = "delegation")
public interface DelegationRepo extends JpaRepository<DelegationModel, Long>, DelegationRepoCustom {

  //Delegations have a combined unique index on area,fromUSer and toProxy
  //db.delegations.createIndex({ "area":1, "fromUser":1, "toProxy":1 }, { unique: true })
  //
  //And their "foreign keys", the ObjectIDs must actually exist in the referenced user and area collections
  //which is checked by DelegationValidator.java

  /**
   * find delegations to a given proxy in the given area
   * @param toProxy ID of a proxy user
   * @param area ID of an area
   * @return all <b>direct</b> delegations to this proxy in that area as a List of DelegationModels (may be empty list)
   */
  List<DelegationModel> findByAreaAndToProxy(@Param("areaId") AreaModel area, @Param("toProxyId") UserModel toProxy);


  //same as  @Query("select d from delegation_model where d.area_id = ?1 and d.from_user_id = ?2 and d.to_proxy_id = ?3")
  //same as  delegationRepo.findAll(Example.of(delegationModel));   returns list of delegations
  DelegationModel findByAreaAndFromUserAndToProxy(AreaModel area, UserModel fromUser, UserModel toProxy);

}
