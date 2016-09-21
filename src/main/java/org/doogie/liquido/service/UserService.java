package org.doogie.liquido.service;

import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CRUD operations for users
 * This calls is an abstraction for the storage backend.
 *
 * In this case we use a spring mongoDB repository for loading and storing laws.
 */
@Service
public class UserService {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private UserRepo userRepo;

  public List<UserModel> findAll() {
    log.trace("getAll Users");
    return userRepo.findAll();
  }
}
