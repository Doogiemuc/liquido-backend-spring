package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.CommentModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Utility methods for comments of a proposal
 */
@Slf4j
@Service
public class CommentService {

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  /**
   * Check if a given comment is already upvoted by the currently logged in user.
   * @param comment a CommentModel
   * @return true  IF this comment is already upvoted by the currently logged in user
   */
  public boolean isUpvotedByCurrentUser(CommentModel comment) {
    Optional<UserModel> currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
    //log.debug("isUpvotedByCurrentUser(comment="+comment.id+") for currentUser= "+currentlyLoggedInUser.getId()+" "+currentlyLoggedInUser.getEmail()+" ===> "+comment.getUpVoters().contains(currentlyLoggedInUser));
    if (!currentlyLoggedInUser.isPresent()) return false;
    return comment.getUpVoters().contains(currentlyLoggedInUser.get());
  }

  /**
   * Check if a given comment is already downvoted by the currently logged in user.
   * @param comment a CommentModel
   * @return true  IF this comment is already downvoted by the currently logged in user
   */
  public boolean isDownvotedByCurrentUser(CommentModel comment) {
    Optional<UserModel> currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
    if (!currentlyLoggedInUser.isPresent()) return false;
    return comment.getDownVoters().contains(currentlyLoggedInUser.get());
  }



}

