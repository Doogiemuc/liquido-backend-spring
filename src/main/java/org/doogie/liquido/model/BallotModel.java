package org.doogie.liquido.model;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.List;

/** POJO Entity that represents a vote that a user has casted */
@Document(collection = "ballots")
public class BallotModel {
  //protected fields are only possible with Google's GSON lib (not with Jackson)
  @Id
  String id;

  @NotNull
  String initialLawId;

  @NotNull
  @NotEmpty
  List<String> voteOrder;

  //TODO: String votersHash;

  public BallotModel() {}

  public BallotModel(String initialLawId, List<String> voteOrder) {
    this.id = null;
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
        '}';
  }

  public String getId() {
    return id;
  }
}
