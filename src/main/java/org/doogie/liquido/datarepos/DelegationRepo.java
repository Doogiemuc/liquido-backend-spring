package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Database abstraction for delegations from a voter to a proxy.
 */
//NOT EXPOSED as REST Service!
public interface DelegationRepo extends CrudRepository<DelegationModel, Long>, DelegationRepoCustom {

  //Delegations have a combined unique index on area,fromUSer
  //  for MongoDB   db.delegations.createIndex({ "area":1, "fromUser":1 }, { unique: true })
  //  for MySQL     ALTER TABLE delegations ADD CONSTRAINT DELEGATION_COMPOSITE_ID UNIQUE (area_id, from_user_id)
	// http://stackoverflow.com/questions/14014086/what-is-difference-between-crudrepository-and-jparepository-interfaces-in-spring
  //
  //And their "foreign keys", the ObjectIDs must actually exist in the referenced user and area collections
  //which is checked by DelegationValidator.java


  /**
   * find all currently assigned proxies of one user in all areas
   * @param fromUser the delegee
   * @return a list of proxies of this delegee
   */
  List<DelegationModel> findByFromUser(@Param("fromUser") UserModel fromUser);

  /**
   * find all delegations to a given proxy in the given area
   * @param toProxy ID of a proxy user
   * @param area ID of an area
   * @return all <b>direct</b> delegations to this proxy in that area as a List of DelegationModels (may be empty list)
   */
  List<DelegationModel> findByAreaAndToProxy(@Param("areaId") AreaModel area, @Param("toProxyId") UserModel toProxy);

  /**
   * find the delegation of a given user in one area
   * @param area the area of the delegation
   * @param fromUser the delegee that delegated his vote to a proxy
   * @return the one delegation in that area or null if that user has no proxy in that area
   */
  DelegationModel findByAreaAndFromUser(AreaModel area, UserModel fromUser);

  //same as  @Query("select d from delegation_model where d.area_id = ?1 and d.from_user_id = ?2 and d.to_proxy_id = ?3")
  //same as  delegationRepo.findOne(Example.of(delegationModel));
  //         delegationRepo.findAll(Example.of(delegationModel));  would return a list of delegations
  DelegationModel findByAreaAndFromUserAndToProxy(AreaModel area, UserModel fromUser, UserModel toProxy);

}
