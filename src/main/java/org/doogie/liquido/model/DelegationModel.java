package org.doogie.liquido.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "delegations")
public class DelegationModel {
  @Id
  String id;

  /** area */
  ObjectId area;

  /** ObjectId of delegee user */
  ObjectId from;

  /** ObjectId of proxy user */
  ObjectId to;

  @LastModifiedDate
  Date updatedAt;

  @CreatedDate
  Date createdAt;

  public ObjectId getFrom() {
    return from;
  }

  public ObjectId getTo() {
    return to;
  }
}
