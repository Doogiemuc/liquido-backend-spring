package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.DelegationModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DelegationRepoImpl implements DelegationRepoCustom {

  @Autowired
  private DelegationRepo delegationRepo;

  public int getNumVotes(String proxyId) {
    List<DelegationModel> delegees = delegationRepo.findByToId(proxyId);
    if (delegees.size() == 0) return 1;
    int numVotes = 1;
    for (DelegationModel delegee : delegees) {
      numVotes += delegationRepo.getNumVotes(delegee.getFromId());
    }
    return numVotes;
  }
}
