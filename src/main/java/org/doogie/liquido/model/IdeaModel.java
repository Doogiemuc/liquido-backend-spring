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

  @DBRef
  AreaModel area;

  //TODO: List<UserModel> supporters;  // need list of all supporters so that no one is allowed to vote twice

  //TODO: configure createBy User for laws: http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  @CreatedBy
  @DBRef
  public UserModel createdBy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public IdeaModel(String title, String description, AreaModel area, UserModel createdBy) {
    this.title = title;
    this.description = description;
    this.area = area;
    this.createdBy = createdBy;
  }

}
