package org.doogie.liquido.model;

import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

/**
 * This projection adds the user and area data into the returned JSON for a delegation
 */
@Projection(name = "delegationProjection", types = { DelegationModel.class })
public interface DelegationProjection {
  //Remember that all default fields must be listed here!
  UserModel getFromUser();
  UserModel getToProxy();
  AreaModel getArea();
  Date getCreatedAt();
  Date getUpdatedAt();
}


/*
  Example of a Delegation as returned by this projection (with inlined user and area)

      {
        "fromUser": {
          "createdAt": "2017-01-18T12:13:21.275+0000",
          "updatedAt": "2017-01-18T12:13:21.275+0000",
          "email": "testuser1@liquido.de",
          "profile": {
            "name": "Test User1",
            "website": "http://www.liquido.de",
            "picture": "/static/img/Avatar_32x32.jpeg"
          }
        },
        "toProxy": {
          "createdAt": "2017-01-18T12:13:21.235+0000",
          "updatedAt": "2017-01-18T12:13:21.235+0000",
          "email": "testuser0@liquido.de",
          "profile": {
            "name": "Test User0",
            "website": "http://www.liquido.de",
            "picture": "/static/img/Avatar_32x32.jpeg"
          }
        },
        "area": {
          "createdAt": "2017-01-18T12:13:21.327+0000",
          "updatedAt": "2017-01-18T12:13:21.327+0000",
          "title": "Area 1",
          "description": "Nice description for test area"
        },
        "createdAt": "2017-01-18T12:13:21.446+0000",
        "updatedAt": "2017-01-18T12:13:21.446+0000",
        "_links": {
          "self": {
            "href": "http://localhost:8090/liquido/v2/delegations/1"
          },
          "delegation": {
            "href": "http://localhost:8090/liquido/v2/delegations/1{?projection}",
            "templated": true
          },
          "area": {
            "href": "http://localhost:8090/liquido/v2/delegations/1/area"
          },
          "fromUser": {
            "href": "http://localhost:8090/liquido/v2/delegations/1/fromUser"
          },
          "toProxy": {
            "href": "http://localhost:8090/liquido/v2/delegations/1/toProxy"
          }
        }
      }


 */
