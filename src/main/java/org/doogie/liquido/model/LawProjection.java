package org.doogie.liquido.model;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * This Spring <a href="http://docs.spring.io/spring-data/rest/docs/current/reference/html/#projections-excerpts.excerpting-commonly-accessed-data">projection</a>
 * inlines the creatdBy user information into the returned JSON for every idea.
 */
@Projection(name = "lawProjection", types = { LawModel.class })
public interface LawProjection {
  //Remember that all default fields must be listed here. Otherwise they won't appear in the JSON!
  //Should I expose the DB _id?  Probably not? http://tommyziegler.com/how-to-expose-the-resourceid-with-spring-data-rest/

  String getTitle();
  String getDescription();
  LawModel.LawStatus getStatus();
  //initial law is not inlined but automatically exposed as a HAL reference under "_links"

  // this will inline the reference to User and Area into the JSON of every idea
  UserModel getCreatedBy();
  AreaModel getArea();

  Date getCreatedAt();
  Date getUpdatedAt();

}

/*  example JSON of exposed REST HATEOAS resource "idea"


 */