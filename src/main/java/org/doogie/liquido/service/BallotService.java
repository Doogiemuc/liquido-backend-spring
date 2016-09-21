package org.doogie.liquido.service;

import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.model.BallotModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * CRUD operations for ballots
 * This class hides all the DB internals.
 */
@Service
public class BallotService {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private BallotRepo ballotRepo;

  /**
   * Store a ballot into the DB
   * @param newBallot
   * @return the stored ballot, including the new ID
   */
  public BallotModel postBallot(BallotModel newBallot) {
    log.trace("postBallot: "+newBallot);
    return ballotRepo.save(newBallot);
    /*
    Document doc = new Document().parse(newBallot.toJson());
    ballotsCollection.insertOne(doc);
    */
  }
}
