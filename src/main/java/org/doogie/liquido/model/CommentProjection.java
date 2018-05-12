package org.doogie.liquido.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;
import java.util.Set;

/**
 * Recursivly return all data about comments and their replies
 */
@Projection(name = "commentProjection", types = { CommentModel.class })
public interface CommentProjection {
  //Remember that all default fields must be listed here!
  Long getId();
  String getComment();
  Set<CommentProjection> getReplies();      // This recursively returns all replies incl. detailed information about authors, created dates etc.

	@Value("#{target.getUpVotes()}")
	int getUpVotes();

	@Value("#{target.getDownVotes()}")
	int getDownVotes();

	/** true, if this comment is already upvoted by the currently logged in user */
	@Value("#{@commentService.isUpvotedByCurrentUser(target)}")
	boolean isUpvotedByCurrentUser();

	/** true, if this comment is already downvoted by the currently logged in user */
	@Value("#{@commentService.isDownvotedByCurrentUser(target)}")
	boolean isDownvotedByCurrentUser();

	UserModel getCreatedBy();
  Date getCreatedAt();
  Date getUpdatedAt();
}

