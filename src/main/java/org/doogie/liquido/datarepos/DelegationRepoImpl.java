package org.doogie.liquido.datarepos;

import org.bson.types.ObjectId;
import org.doogie.liquido.model.DelegationModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Implementation of custom query methods for delegations.
 */
public class DelegationRepoImpl implements DelegationRepoCustom {

  @Autowired
  private DelegationRepo delegationRepo;

  public int getNumVotes(String proxyIdAsStr, String areaIdAsStr) {
    return this.getNumVotes(new ObjectId(proxyIdAsStr), new ObjectId(areaIdAsStr));
  }

  /** recursively calculate number of votes that a proxy has, including his own one */
  public int getNumVotes(ObjectId proxyId, ObjectId areaId) {
    List<DelegationModel> delegees = delegationRepo.findByToAndArea(proxyId, areaId);
    if (delegees.size() == 0) return 1;
    int numVotes = 1;
    for (DelegationModel delegee : delegees) {
      numVotes += delegationRepo.getNumVotes(delegee.getFrom(), areaId);
    }
    return numVotes;
  }
}
