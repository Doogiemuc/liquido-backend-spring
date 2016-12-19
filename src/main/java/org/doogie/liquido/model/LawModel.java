package org.doogie.liquido.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
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

  /** Reference to initial proposal. May be reference to self */
  @NotNull
  ObjectId initialLaw;

  public int status;

  //TODO: configure createBy User for laws: http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
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

}
