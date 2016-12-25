package org.doogie.liquido.datarepos;

import org.bson.types.ObjectId;
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

  // this will inline the DBRef to UserModel into the JSON of every idea
  UserModel getCreatedBy();
}

/*  example JSON of exposed REST HATEOAS resource "idea"


     {
        "title": "Test Title",
        "description": "Some very nice idea description",
        "createdBy": {
          "email": "testuser1@liquido.de",
          "passwordHash": "$2a$10$39c7ORNl9ZI8ckwpNLLlPuMAh.IzAxuZTvQj9sWV38pqnRN.m7izO",
          "profile": {
            "name": "Test User1",
            "website": "http://www.liquido.de",
            "picture": "/static/img/Avatar_32x32.jpeg"
          },
          "updatedAt": "2016-11-17T20:53:48.801+0000",
          "createdAt": "2016-11-16T20:53:48.801+0000"
        },
        "updatedAt": "2016-12-25T19:23:50.448+0000",
        "createdAt": "2016-12-25T19:23:50.448+0000",
        "_links": {
          "self": {
            "href": "http://localhost:8080/liquido/v2/ideas/58601cc69f6c11127873b254"
          },
          "ideaModel": {
            "href": "http://localhost:8080/liquido/v2/ideas/58601cc69f6c11127873b254{?projection}",
            "templated": true
          },
          "createdBy": {
            "href": "http://localhost:8080/liquido/v2/ideas/58601cc69f6c11127873b254/createdBy"
          }
        }
      }


 */