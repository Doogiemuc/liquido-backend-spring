package org.doogie.liquido.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "delegations")
public class DelegationModel {
  @Id
  String id;

  UserModel from;
  UserModel to;
}
