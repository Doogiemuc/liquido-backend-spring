package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * This Spring <a href="http://docs.spring.io/spring-data/rest/docs/current/reference/html/#projections-excerpts.excerpting-commonly-accessed-data">projection</a>
 * inlines the creatdBy user information into the returned JSON for every idea.
 */
@Projection(name = "ideaWithCreatedByUser", types = { IdeaModel.class })
interface IdeaProjection {
  //Remember that all default fields must be listed here. Otherwise they won't appear in the JSON!
  //Should I expose the DB _id?  Probably not? http://tommyziegler.com/how-to-expose-the-resourceid-with-spring-data-rest/

  String getTitle();
  String getDescription();
  Date getCreatedAt();
  Date getUpdatedAt();

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