package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;

import java.util.List;

//TODO: not yet used
/**
 * Custom query and search methods for laws.
 */
public interface LawRepoCustom {

  /**
   * Fetch proposals that were recently supported by user
   * @param user any UserModel
   * @return list of LawModels that this user liked.
   */
  List<LawModel> findSupportedProposals(UserModel user);

}
