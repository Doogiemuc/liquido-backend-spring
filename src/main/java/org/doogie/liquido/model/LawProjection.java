package org.doogie.liquido.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * This Spring <a href="http://docs.spring.io/spring-data/rest/docs/current/reference/html/#projections-excerpts.excerpting-commonly-accessed-data">projection</a>
 * inlines the createdBy user information into the returned JSON for every proposal
 * and adds some more date fields relevant for an idea or proposal.
 *
 * Keep in mind that projections are only used, when a LawModel is returned inside a list, e.g. in "_embedded" array.
 * When exactly one LawModel is returned e.g. by GET /laws/4711 , then Spring Data REST will always show the original LawModel as JSON. Because only then can the client send PUT requests with this data.
 */
@Projection(name = "lawProjection", types = { LawModel.class })
public interface LawProjection {
  // Remember that all default fields must be listed here. Otherwise they won't appear in the JSON!
  // Reference to poll is automatically exposed as HATEOAS _link

  Long getId();

  String getTitle();

  String getDescription();

  LawModel.LawStatus getStatus();

  AreaModel getArea();  // always inline information about the area  directly into the JSON

  PollModel getPoll();  // always inline information about poll

  // To fetch comments user CommentProjection of CommentModel
  //Set<CommentModel> getComments();

  LocalDateTime getReachedQuorumAt();

  Date getCreatedAt();

  Date getUpdatedAt();

  /**
   * Inline the reference to creator of this idea
   * Keep in mind, that the creator is not counted as supporter!
   */
  UserModel getCreatedBy();

  /** true, if this idea, proposal or proposal is already supported by the currently logged in user */
  @Value("#{@lawService.isSupportedByCurrentUser(target)}")
  boolean isSupportedByCurrentUser();

  int getNumSupporters();

  //MAYBE: int getNumComments();
}