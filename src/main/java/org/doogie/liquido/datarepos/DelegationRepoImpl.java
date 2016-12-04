package org.doogie.liquido.datarepos;

import org.bson.types.ObjectId;
import org.doogie.liquido.model.DelegationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Implementation of custom query methods for delegations.
 */
public class DelegationRepoImpl implements DelegationRepoCustom {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  private DelegationRepo delegationRepo;

  public int getNumVotes(String proxyIdAsStr, String areaIdAsStr) {
    return this.getNumVotes(new ObjectId(proxyIdAsStr), new ObjectId(areaIdAsStr));
  }

  /**
   * Calculate number of votes that a user has,
   * by recursively checking his proxies (and their proxies ...)
   * @param userId a user's ID
   * @param areaId check proxies in that area.
   * @return number of votes of the this user (including his own one)
   */
  public int getNumVotes(ObjectId userId, ObjectId areaId) {
    List<DelegationModel> delegations = delegationRepo.findByToProxyAndArea(userId, areaId);
    if (delegations.size() == 0) return 1;
    int numVotes = 1;
    for (DelegationModel delegation : delegations) {
      //BUGFIX: interrupt recursion, when there are circular delegations in the DB  (GRRRR)
      numVotes += delegationRepo.getNumVotes(delegation.getFromUser(), areaId);
    }
    return numVotes;
  }

}
