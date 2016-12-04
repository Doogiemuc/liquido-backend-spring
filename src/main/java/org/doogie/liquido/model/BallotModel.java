package org.doogie.liquido.model;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** POJO Entity that represents a vote that a user has casted */
@Document(collection = "ballots")
public class BallotModel {
  //protected fields are only possible with Google's GSON lib (not with Jackson)
  @Id
  private String id;

  @NotNull
  public ObjectId initialLawId;

  @NotNull
  @NotEmpty
  public List<ObjectId> voteOrder;

  /** encrypted information about voter that casted this ballot */
  @NotNull
  @NotEmpty
  public String voterHash;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  /** empty default constructor. Needed for deserialisation from HttpRequests */
  public BallotModel() {}

  public BallotModel(String voterHash, ObjectId initialLawId, List<ObjectId> voteOrder) {
    this.id = null;
    this.voterHash = voterHash;
    this.initialLawId = initialLawId;
    this.voteOrder = voteOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BallotModel that = (BallotModel) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (!initialLawId.equals(that.initialLawId)) return false;
    return voteOrder != null ? voteOrder.equals(that.voteOrder) : that.voteOrder == null;

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (initialLawId != null ? initialLawId.hashCode() : 0);
    result = 31 * result + (voteOrder != null ? voteOrder.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "BallotModel{" +
        "id='" + id + '\'' +
        ", initialLawId='" + initialLawId + '\'' +
        ", voteOrder=" + voteOrder +
        ", updatedAt=" + updatedAt +
        ", createdAt=" + createdAt +
        '}';
  }

  public String getId() {
    return id;
  }

  public String getVoterHash() {
    return voterHash;
  }
}
