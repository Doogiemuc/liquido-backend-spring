package org.doogie.liquido.model;

import org.doogie.liquido.services.IdeaUtil;
import org.springframework.beans.factory.annotation.Value;
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

  Long getId();     // yes I want to expose the internal ID. Makes routing in the client much easier, than using the URIs from _links.self.href
  String getTitle();
  String getDescription();
  Date getCreatedAt();
  Date getUpdatedAt();
  int getNumSupporters();

  /** see {@link IdeaUtil} */
  @Value("#{@ideaUtil.isSupportedByCurrentUser(target)}")   // Spring Expression language (SpEL) FTW
  boolean isSupportedByCurrentUser();

  // this will inline the reference to User and Area into the JSON of every idea
  UserModel getCreatedBy();
  AreaModel getArea();
}

/*  example JSON of exposed REST HATEOAS resource "idea"

{
  "_embedded": {
    "ideas": [
      {
        "id": 27,
        "title": "Idea 0",
        "description": "Lorem ipsum dolor sit amet, consectetur adipiscing",
        "createdBy": {
          "createdAt": "2017-02-16T15:42:07.036+0000",
          "updatedAt": "2017-02-16T15:42:07.036+0000",
          "email": "testuser0@liquido.de",
          "profile": {
            "name": "Test User0",
            "website": "http://www.liquido.de",
            "picture": "/static/img/Avatar_32x32.jpeg"
          }
        },
        "supportedByCurrentUser": true,
        "area": {
          "createdAt": "2017-02-16T15:42:07.136+0000",
          "updatedAt": "2017-02-16T15:42:07.136+0000",
          "title": "Area 0",
          "description": "Nice description for test area #0",
          "creator": {
            "createdAt": "2017-02-16T15:42:07.036+0000",
            "updatedAt": "2017-02-16T15:42:07.036+0000",
            "email": "testuser0@liquido.de",
            "profile": {
              "name": "Test User0",
              "website": "http://www.liquido.de",
              "picture": "/static/img/Avatar_32x32.jpeg"
            }
          }
        },
        "numSupporters": 0,
        "updatedAt": "2017-02-16T15:42:07.556+0000",
        "createdAt": "2017-02-16T15:42:07.556+0000",
        "_links": {
          "self": {
            "href": "http://localhost:8080/liquido/v2/ideas/27"
          },
          "idea": {
            "href": "http://localhost:8080/liquido/v2/ideas/27{?projection}",
            "templated": true
          },
          "supporters": {
            "href": "http://localhost:8080/liquido/v2/ideas/27/supporters"
          },
          "createdBy": {
            "href": "http://localhost:8080/liquido/v2/ideas/27/createdBy"
          },
          "area": {
            "href": "http://localhost:8080/liquido/v2/ideas/27/area"
          }
        }
      },

 */