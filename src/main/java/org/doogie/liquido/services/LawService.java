package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.CommentRepo;
import org.doogie.liquido.datarepos.LawEventHandler;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.CommentModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

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
  CommentRepo commentRepo;

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
    Optional<UserModel> currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
    if (!currentlyLoggedInUser.isPresent()) return false;
    return law.getSupporters().contains(currentlyLoggedInUser.get());
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
    //TODO: temporarily disabled:   log.info("addSupporter: "+supporter.getEmail()+"(id="+supporter.getId()+") now supports "+idea);
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



  private class CompareByRecentComments implements Comparator<LawModel> {
    final Map<Long, Long> numRecentComments;

    public CompareByRecentComments(Map<Long, Long> numRecentComments) {
      this.numRecentComments = numRecentComments;
    }

    @Override
    public int compare(LawModel o1, LawModel o2) {
      long count1 = numRecentComments.getOrDefault(o1.getId(), 0L);
      long count2 = numRecentComments.getOrDefault(o2.getId(), 0L);
      return Long.compare(count2, count1);   // need to reverse the order
    }
  }

  /**
   * Get recently discussed proposals. (Only proposals can be discussed!)
   * @param max maximum number of proposals to return
   * @return most discussed proposals sorted by their number of recent comments
   */
  public List<LawModel> getRecentlyDiscussed(int max) {
    Map<Long, Long> numRecentComments = new HashMap<>();
    Set<LawModel> mostDiscussedProposals = new HashSet<>();
    //Iterable<CommentModel> recentComments = commentRepo.findAll(Sort.by(Sort.Order.desc("createdAt")));   // newest comments first
    Iterable<CommentModel> recentComments = commentRepo.findAll(PageRequest.of(0, 50, Sort.by(Sort.Order.desc("createdAt"))));  // when a line of code ends with four closing brackets, then you do have to many factory methods ! :-)
    for (CommentModel c: recentComments) {
      mostDiscussedProposals.add(c.getLaw());
      Long key   = c.getLaw().getId();
      Long count = numRecentComments.getOrDefault(key, 0L);
      numRecentComments.put(key, count + 1);
    }
    List<LawModel> sortedProposals = mostDiscussedProposals.stream()
        .sorted(new CompareByRecentComments(numRecentComments))
        .limit(max)
        .collect(Collectors.toList());
    return sortedProposals;
  }

}

