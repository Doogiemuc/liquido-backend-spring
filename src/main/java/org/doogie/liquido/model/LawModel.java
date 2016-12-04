package org.doogie.liquido.model;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Document(collection = "laws")
public class LawModel {
  @Id
  private String id;

  @NotNull
  @NotEmpty
  public String title;

  @NotNull
  @NotEmpty
  public String description;

  public int status;

  @CreatedBy
  public ObjectId createdBy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public LawModel(String title, String description, int status) {
    this.title = title;
    this.description = description;
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LawModel lawModel = (LawModel) o;

    if (status != lawModel.status) return false;
    if (title != null ? !title.equals(lawModel.title) : lawModel.title != null) return false;
    if (description != null ? !description.equals(lawModel.description) : lawModel.description != null) return false;
    return createdBy != null ? createdBy.equals(lawModel.createdBy) : lawModel.createdBy == null;

  }

  @Override
  public int hashCode() {
    int result = title != null ? title.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + status;
    result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "LawModel{" +
        "title='" + title + '\'' +
        ", description='" + description + '\'' +
        ", status=" + status +
        ", createdBy=" + createdBy +
        '}';
  }
}
