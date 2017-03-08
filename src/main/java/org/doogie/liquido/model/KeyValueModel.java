package org.doogie.liquido.model;

import lombok.Data;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "keyValue")
public class KeyValueModel {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long id;

  @NonNull
  @NotNull
  @NotEmpty
  public String key;

  @NonNull
  public String value;
}
