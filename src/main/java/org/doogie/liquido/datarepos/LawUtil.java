package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LawUtil {

  @Autowired
  LawRepo lawRepo;

  /**
   * get the number of competing laws that have the same initial law as the passed law
   * @param law any LawModel (not necessarily the initial law)
   * @return the total number of competing laws (including the passed one)
   */
  public long getNumCompetingProposals(LawModel law) {
    if (law == null) return 0;
    return lawRepo.countByInitialLaw(law.getInitialLaw());               //this is used in LawProjection.java
  }

}

