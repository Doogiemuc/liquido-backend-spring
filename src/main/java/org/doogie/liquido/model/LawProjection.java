package org.doogie.liquido.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * This Spring <a href="http://docs.spring.io/spring-data/rest/docs/current/reference/html/#projections-excerpts.excerpting-commonly-accessed-data">projection</a>
 * inlines the creatdBy user information into the returned JSON for every law
 * and adds some more date fields relevant for a proposal or law.
 */
@Projection(name = "lawProjection", types = { LawModel.class })
public interface LawProjection {
  // Remember that all default fields must be listed here. Otherwise they won't appear in the JSON!
  // Reference to poll is automatically exposed as HATEOAS _link

  Long getId();

  String getTitle();

  String getDescription();

  LawModel.LawStatus getStatus();

  AreaModel getArea();  // inline information about the area

  Date getReachedQuorumAt();

  Date getCreatedAt();

  Date getUpdatedAt();

  /**
   * Inline the reference to creator of this idea
   * Keep in mind, that the creator is not counted as supporter!
   */
  UserModel getCreatedBy();

  /** true, if this idea, proposal or law is already supported by the currently logged in user */
  @Value("#{@lawService.isSupportedByCurrentUser(target)}")
  boolean isSupportedByCurrentUser();

  int getNumSupporters();
  /*
  {
    if (this.supporters == null) return 0;
    return this.supporters.size();
  }
  */

}