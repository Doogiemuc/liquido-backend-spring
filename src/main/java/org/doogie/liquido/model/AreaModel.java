package org.doogie.liquido.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Document(collection = "areas")
public class AreaModel {
  @Id
  public String id;

  // public fields are automatically exposed in REST endpoint
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

}
