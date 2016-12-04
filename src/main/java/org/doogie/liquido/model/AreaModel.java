package org.doogie.liquido.model;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Document(collection = "areas")
public class AreaModel {
  @Id
  private String id;

  // public fields are automatically exposed in REST enpoint
  @NotNull
  @NotEmpty
  public String title;

  @NotNull
  @NotEmpty
  public String description;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public AreaModel(String title, String description) {
    this.title = title;
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AreaModel areaModel = (AreaModel) o;

    if (!title.equals(areaModel.title)) return false;
    if (!description.equals(areaModel.description)) return false;
    if (!updatedAt.equals(areaModel.updatedAt)) return false;
    return createdAt.equals(areaModel.createdAt);

  }

  @Override
  public int hashCode() {
    int result = title.hashCode();
    result = 31 * result + description.hashCode();
    result = 31 * result + updatedAt.hashCode();
    result = 31 * result + createdAt.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "AreaModel{" +
        "id='" + id + '\'' +
        ", title='" + title + '\'' +
        ", description='" + description + '\'' +
        ", updatedAt=" + updatedAt +
        ", createdAt=" + createdAt +
        '}';
  }

  public String getId() {
    return id;
  }


}
