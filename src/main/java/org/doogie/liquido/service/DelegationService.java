package org.doogie.liquido.service;

import org.doogie.liquido.datarepos.DelegationRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * CRUD operations for delegations
 * This calls is an abstraction for the storage backend.
 *
 * In this case we use a spring mongoDB repository for loading and storing laws.
 */
@Service
public class DelegationService {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private DelegationRepo delegationRepo;

  public int getNumberOfVotes(String userId, String areaId) {
    log.trace("getNumberOfVotes");
    int num = 1;

    return num;
  }
}
