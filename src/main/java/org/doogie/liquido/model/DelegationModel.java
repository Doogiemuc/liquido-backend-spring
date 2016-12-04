package org.doogie.liquido.model;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Document(collection = "delegations")
public class DelegationModel {
  @Id
  private String id;

  /** area */
  @NotNull
  @NotEmpty
  public ObjectId area;

  /** ObjectId of delegee user */
  @NotNull
  @NotEmpty
  public ObjectId fromUser;

  /** ObjectId of proxy user */
  @NotNull
  @NotEmpty
  public ObjectId toProxy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public DelegationModel(String fromUserId, String toProxyId, String areaId) {
    this.fromUser = new ObjectId(fromUserId);
    this.toProxy  = new ObjectId(toProxyId);
    this.area     = new ObjectId(areaId);
    this.createdAt = new Date();
    this.updatedAt = new Date();
  }

  public ObjectId getFromUser() {
    return fromUser;
  }

  public ObjectId getToProxy() {
    return toProxy;
  }

  public ObjectId getArea() { return area; }
}
