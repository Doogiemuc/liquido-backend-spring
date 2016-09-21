package org.doogie.liquido.service;

import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.LawModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CRUD operations for laws
 * This calls is an abstraction for the storage backend.
 *
 * In this case we use a spring mongoDB repository for loading and storing laws.
 */
@Service
public class LawService {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private LawRepo lawRepo;

  public List<LawModel> findAll() {
    log.trace("getAll Laws");
    return lawRepo.findAll();
  }
}
