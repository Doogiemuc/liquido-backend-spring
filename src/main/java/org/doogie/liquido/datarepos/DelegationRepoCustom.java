package org.doogie.liquido.datarepos;

/**
 * Custom query mehtods for delegations.
 */
public interface DelegationRepoCustom {

  /**
   * calculate the number of votes that a proxy may cast, ie. the sum of all his (direct and transitive) delegates.
   * @param userId ID of proxy
   * @return the number of votes this user may cast (including his own one!)
   */
  int getNumVotes(String userId);

}
