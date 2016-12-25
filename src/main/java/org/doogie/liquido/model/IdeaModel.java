package org.doogie.liquido.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Document(collection = "ideas")
public class IdeaModel {
  @Id
  public String id;

  @NotNull
  @NotEmpty
  public String title;

  @NotNull
  @NotEmpty
  public String description;

  //TODO: configure createBy User for laws: http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  @CreatedBy
  @DBRef
  public UserModel createdBy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public IdeaModel(String title, String description, UserModel createdBy) {
    this.title = title;
    this.description = description;
    this.createdBy = createdBy;
  }

}
