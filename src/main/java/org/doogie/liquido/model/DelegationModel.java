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
  public ObjectId area;

  /** ObjectId of delegee user */
  @NotNull
  public ObjectId fromUser;

  /** ObjectId of proxy user */
  @NotNull
  public ObjectId toProxy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public DelegationModel() { }

  public DelegationModel(ObjectId area, ObjectId fromUser, ObjectId toProxy, Date updatedAt, Date createdAt) {
    this.area = area;
    this.fromUser = fromUser;
    this.toProxy = toProxy;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public ObjectId getFromUser() {
    return fromUser;
  }

  public ObjectId getToProxy() {
    return toProxy;
  }

  public ObjectId getArea() { return area; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DelegationModel that = (DelegationModel) o;

    if (!area.equals(that.area)) return false;
    if (!fromUser.equals(that.fromUser)) return false;
    return toProxy.equals(that.toProxy);
  }

  @Override
  public int hashCode() {
    int result = area.hashCode();
    result = 31 * result + fromUser.hashCode();
    result = 31 * result + toProxy.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "DelegationModel{" +
        "id='" + id + '\'' +
        ", area=" + area +
        ", fromUser=" + fromUser +
        ", toProxy=" + toProxy +
        ", updatedAt=" + updatedAt +
        ", createdAt=" + createdAt +
        '}';
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
