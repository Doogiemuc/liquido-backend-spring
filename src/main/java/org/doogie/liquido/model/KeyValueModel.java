package org.doogie.liquido.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * General purpose model for key=value properties.
 * Mainly used for global configuration variables that need to be persisted.
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "keyValue")
public class KeyValueModel {
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  public Long id;

  @NonNull
  @NotNull
  @Column(unique = true, name="keyCol")
  public String key;

  @NonNull
  @Column(name = "valueCol")  // BUGFIX: MySql has a problem with a column name "value", because "value" is a reserved word in MySql.
  public String value;
}
