package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.LawEventHandler;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Utility methods for a Law. These are for example used by {@link org.doogie.liquido.model.LawProjection}
 */
@Slf4j
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
    return law.getPoll().getNumCompetingProposals();
  }

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  LiquidoProperties props;

  //@Autowired
  //PollService pollService;

  /**
   * Check if a given idea is already supported by the currently logged in user.
   * Remark: Of course one could say that an idea is implicitly supported by its creator. But that is not counted in the list of supporters,
   * because an idea needs "external" supporters, to become a proposal for a law.
   * @param law a LawModel
   * @return true  IF this idea is supported by the currently logged in user
   *         false IF there is no user logged in
   */
  public boolean isSupportedByCurrentUser(LawModel law) {
    //this is used in LawProjection.java
    UserModel currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
    return law.getSupporters().contains(currentlyLoggedInUser);
  }

  /**
   * When an idea reaches its quorum, then it becomes a proposal.
   * Called from {@link LawEventHandler}
   * @param idea an idea where a supporter has been added.
   */
  public void checkQuorum(LawModel idea) {
    if (idea != null &&
        idea.getStatus().equals(LawModel.LawStatus.IDEA) &&
        idea.getNumSupporters() >= props.getInt(LiquidoProperties.KEY.LIKES_FOR_QUORUM) ) {
      log.info("Idea (id="+idea.getId()+") '"+idea.getTitle()+"' reached its quorum with "+idea.getNumSupporters()+" supporters.");
      idea.setStatus(LawModel.LawStatus.PROPOSAL);
      idea.setReachedQuorumAt(new Date());
      lawRepo.save(idea);
    }
    //TODO: What happens with a law when a supporter gets removed?
  }
}

