package org.doogie.liquido.services;

import org.doogie.liquido.model.LawModel;
import org.springframework.stereotype.Component;

/**
 * Utility methods for a Law. These are for example used by {@link org.doogie.liquido.model.LawProjection}
 */
@Component
public class LawService {

  /**
   * get the number of competing laws that have the same initial law as the passed law.
   * This is used in {@link org.doogie.liquido.model.LawProjection}
   * @param law a LawModel (not necessarily the initial proposal)
   * @return the total number of competing proposals (including the passed one)
   * @See org.doogie.liquido.services.PollService#getNumCompetingProposals
   */
  public long getNumCompetingProposals(LawModel law) {
    if (law == null || law.getPoll() == null) return 0;
    return law.getPoll().getNumCompetingProposals();               //this is used in LawProjection.java
  }


}

