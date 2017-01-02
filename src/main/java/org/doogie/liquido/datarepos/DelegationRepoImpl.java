package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
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

  /**
   * Calculate number of votes that a user has,
   * by recursively checking his proxies (and their proxies ...)
   * @param user a user
   * @param area check proxies in that area.
   * @return number of votes of the this user (including his own one)
   */
  public int getNumVotes(AreaModel area, UserModel user) {
    return this.getNumVotesInternal(area, user, 0);
  }

  private int getNumVotesInternal(AreaModel area, UserModel user, long recursionDepth) {
    if (recursionDepth >= Long.MAX_VALUE-1000) {
      throw new RuntimeException("To many transitive delegations. There seems to be a circular delegation user: "+user);
    }
    List<DelegationModel> delegations = delegationRepo.findByAreaAndToProxy(area, user);
    if (delegations.size() == 0) return 1;
    int numVotes = 1;
    for (DelegationModel delegation : delegations) {
      //BUGFIX: interrupt recursion, when there are circular delegations in the DB  (GRRRR)
      numVotes += this.getNumVotesInternal(area, delegation.getFromUser(), recursionDepth+1);
    }
    return numVotes;
  }

}
