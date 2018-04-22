package org.doogie.liquido.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * General purpose model for key=value properties.
 * Mainly used for global configuration variables that are stored in the DB.
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "keyValue")
public class KeyValueModel extends BaseModel {
  @NonNull
  @NotNull
  @NotEmpty
  @Column(unique = true)
  public String key;

  @NonNull
  public String value;
}
