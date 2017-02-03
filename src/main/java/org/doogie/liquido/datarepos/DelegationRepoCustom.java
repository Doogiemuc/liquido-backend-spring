package org.doogie.liquido.datarepos;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;

/**
 * Custom query methods for delegations.
 */
public interface DelegationRepoCustom {

  /**
   * calculate the number of votes that a proxy may cast, ie. the sum of all his (direct and transitive) delegates.
   * @param user ObjectId of proxy
   * @param area ObjectId of area
   * @return the number of votes this user may cast (including his own one!)
   */
  long getNumVotes(AreaModel area, UserModel user);

}
