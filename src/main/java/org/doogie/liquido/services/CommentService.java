package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.LawEventHandler;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.CommentModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Utility methods for comments of a proposal
 */
@Slf4j
@Component
public class CommentService {

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  /**
   * Check if a given comment is already upvoted by the currently logged in user.
   * @param comment a CommentModel
   * @return true  IF this comment is already upvoted by the currently logged in user
   */
  public boolean isUpvotedByCurrentUser(CommentModel comment) {
    UserModel currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
    log.debug("isUpvotedByCurrentUser(comment="+comment.id+") for currentUser= "+currentlyLoggedInUser.getId()+" "+currentlyLoggedInUser.getEmail()+" ===> "+comment.getUpVoters().contains(currentlyLoggedInUser));

    return comment.getUpVoters().contains(currentlyLoggedInUser);
  }

  /**
   * Check if a given comment is already downvoted by the currently logged in user.
   * @param comment a CommentModel
   * @return true  IF this comment is already downvoted by the currently logged in user
   */
  public boolean isDownvotedByCurrentUser(CommentModel comment) {
    UserModel currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
    return comment.getDownVoters().contains(currentlyLoggedInUser);
  }



}

