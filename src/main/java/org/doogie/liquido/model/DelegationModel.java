package org.doogie.liquido.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "delegations")
public class DelegationModel {
  @Id
  String id;

  /** id of delegee */
  String fromId;

  /** id of proxy */
  String toId;

  public String getFromId() {
    return fromId;
  }

  public String getToId() {
    return toId;
  }
}
