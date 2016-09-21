package org.doogie.liquido.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "laws")
public class LawModel {
  @Id
  String id;

  String title;
  String description;
  int status;

  UserModel createdBy;

}
