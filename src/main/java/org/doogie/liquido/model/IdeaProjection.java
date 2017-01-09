package org.doogie.liquido.model;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;
//
//
//






//TODO: IMPORTANT Create a @BasePathAwareController  IdeaRestController{...} that returns a IdeaJSON with all related entites embedded and also with properties like getSupportedByCurrentUser that can't be calculated here (currenlty logged in user not known in IdeaModel!!!)











/**
 * This Spring <a href="http://docs.spring.io/spring-data/rest/docs/current/reference/html/#projections-excerpts.excerpting-commonly-accessed-data">projection</a>
 * inlines the creatdBy user information into the returned JSON for every idea.
 *
 * Projection classes must be in the same package as the *Model.java class! Otherwise projections are not publicly accessible.
 * https://stackoverflow.com/questions/30220333/why-is-an-excerpt-projection-not-applied-automatically-for-a-spring-data-rest-it
 */
@Projection(name = "ideaProjection", types = { IdeaModel.class })
public interface IdeaProjection {
  //Remember that all default fields must be listed here. Otherwise they won't appear in the JSON!
  //Should I expose the DB _id?  Probably not? http://tommyziegler.com/how-to-expose-the-resourceid-with-spring-data-rest/

  String getTitle();
  String getDescription();
  Date getCreatedAt();
  Date getUpdatedAt();
  int getNumSupporters();

  /**
   * @return true  if this idea is supported by the currently logged in user or
   *               if this idea is created by the currently logged in user,
   *         false if there is no user logged in
   */
  boolean getSupportedByCurrentUser();

  // this will inline the reference to User and Area into the JSON of every idea
  UserModel getCreatedBy();
  AreaModel getArea();
}

/*  example JSON of exposed REST HATEOAS resource "idea"


     {
      "title" : "Test Title by User1 1482763815625",
      "description" : "Some very nice idea description",
      "area" : {
        "title" : "Area 1",
        "description" : "Department/Area of interest or ministry 1",
        "updatedAt" : "2016-10-16T20:53:49.717+0000",
        "createdAt" : "2016-10-16T20:53:49.717+0000"
      },
      "createdBy" : {
        "email" : "testuser1@liquido.de",
        "profile" : {
          "name" : "Test User1",
          "website" : "http://www.liquido.de",
          "picture" : "/static/img/Avatar_32x32.jpeg"
        },
        "updatedAt" : "2016-11-17T20:53:48.801+0000",
        "createdAt" : "2016-11-16T20:53:48.801+0000"
      },
      "updatedAt" : "2016-12-26T14:50:15.635+0000",
      "createdAt" : "2016-12-26T14:50:15.635+0000",
      "_links" : {
        "self" : {
          "href" : "http://localhost:8090/liquido/v2/ideas/58612e279f6c1110a463b3a8"
        },
        "idea" : {
          "href" : "http://localhost:8090/liquido/v2/ideas/58612e279f6c1110a463b3a8{?projection}",
          "templated" : true
        },
        "area" : {
          "href" : "http://localhost:8090/liquido/v2/ideas/58612e279f6c1110a463b3a8/area"
        },
        "createdBy" : {
          "href" : "http://localhost:8090/liquido/v2/ideas/58612e279f6c1110a463b3a8/createdBy"
        }
      }
    }

 */