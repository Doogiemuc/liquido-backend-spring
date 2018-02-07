package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Implementation for custom law query and search methods
 * Currently not yet used
 */
public class LawRepoImpl implements LawRepoCustom {

  @Autowired
  LawRepo lawRepo;

}
