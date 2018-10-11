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

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Utility methods for a Law. These are for example used by {@link org.doogie.liquido.model.LawProjection}
 */
@Slf4j
@Component
public class LawService {

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  LiquidoProperties props;

  /**
   * Check if a given idea is already supported by the currently logged in user.
   * Remark: The creator is not counted as a supporter! Of course one could say that an idea is implicitly supported by its creator. But that is not counted in the list of supporters,
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
 * Add a supporter to an idea or proposal.
 * This is actually a quite interesting algorithm, because the initial creator of the idea must not be added as supporter
 * and supporters must not be added twice.
 * @param supporter the user that 'likes' this idea and wants to support and discuss it.
 * @param idea the idea to add to
 * @return the saved idea
 */
  public LawModel addSupporter(@NotNull UserModel supporter, @NotNull LawModel idea) {
    if (idea.getSupporters().contains(supporter)) return idea;   // Do not add supporter twice
    if (idea.getCreatedBy().equals(supporter)) return idea;      // Must not support your own idea
    log.info("addSupporter: "+supporter.getEmail()+"(id="+supporter.getId()+") now supports "+idea);
    idea.getSupporters().add(supporter);
    idea = lawRepo.save(idea);
    idea = checkQuorum(idea);
    return idea;
  }

  /**
   * When an idea reaches its quorum, then it becomes a proposal.
   * This is automatically called from {@link LawEventHandler} when supporter has been added via REST: POST /laws/4711/supporters
   * @param idea an idea where a supporter has been added.
   */
  public LawModel checkQuorum(LawModel idea) {
    if (idea != null &&
        idea.getStatus().equals(LawModel.LawStatus.IDEA) &&
        idea.getNumSupporters() >= props.getInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL) ) {
      log.info("Idea (id="+idea.getId()+") '"+idea.getTitle()+"' reached its quorum with "+idea.getNumSupporters()+" supporters.");
      idea.setStatus(LawModel.LawStatus.PROPOSAL);
      idea.setReachedQuorumAt(new Date());
      return lawRepo.save(idea);
    }
    return idea;
    //TODO: What happens with a law when a supporter gets removed?
  }

}

