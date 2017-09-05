package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Implementation for custom law query and searach methods
 */
public class LawRepoImpl implements LawRepoCustom {

  @Autowired
  LawRepo lawRepo;

  public List<LawModel> findSupportedProposals(UserModel user) {
    return lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.PROPOSAL, user);
  }

}
