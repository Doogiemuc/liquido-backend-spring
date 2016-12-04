package org.doogie.liquido.datarepos;

import org.bson.types.ObjectId;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * Custom query methods for delegations.
 */
public interface DelegationRepoCustom {

  /**
   * Calculate the number of votes that a proxy may cast, ie. the sum of all his (direct and transitive) delegates.
   * This method simply converts the string to real ObjectIds and the delegates to {@link #getNumVotes(ObjectId, ObjectId)}.
   * @param userIdAsStr MongoDB ObjectId of proxy in HEX
   * @param areaIdAsStr MongoDB ObjectId of area in HEX
   * @return the number of votes this user may cast (including his own one!)
   */
  int getNumVotes(String userIdAsStr, String areaIdAsStr);

  /**
   * calculate the number of votes that a proxy may cast, ie. the sum of all his (direct and transitive) delegates.
   * @param userId ObjectId of proxy
   * @param areaId ObjectId of area
   * @return the number of votes this user may cast (including his own one!)
   */
  int getNumVotes(ObjectId userId, ObjectId areaId);

}
