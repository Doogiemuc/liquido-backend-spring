package org.doogie.liquido.services;

import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdeaUtil {

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
  LiquidoProperties props;

  @Autowired
  PollService pollService;

  /**
   * Check if a given idea is already supported by the currently logged in user.
   * Remark: Of course one could say that an idea is implicitly supported by its creator. But that is not counted in the list of supporters,
   * because an idea needs "external" supporters, to become a suggestion for a law.
   * @param idea an IdeaModel
   * @return  true IF this idea is supported by the currently logged in user
   *         false IF there is no user logged in
   */
  public boolean isSupportedByCurrentUser(IdeaModel idea) {
    //this is used in IdeaProjection.java
    try {
      UserModel currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
      return idea.getSupporters().contains(currentlyLoggedInUser);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Checks if an idea has reached its quorum.
   * Called from {@link org.doogie.liquido.datarepos.IdeaEventHandler}
   * Will call {@link PollService#ideaReachesQuorum(IdeaModel)} when idea has enough likes.
   * @param idea the IdeaModel to check
   */
  public void checkQuorum(IdeaModel idea) {
    if (idea.getNumSupporters() == props.getInt(LiquidoProperties.KEY.LIKES_FOR_QUORUM)) {
      pollService.ideaReachesQuorum(idea);
    }
  }
}
