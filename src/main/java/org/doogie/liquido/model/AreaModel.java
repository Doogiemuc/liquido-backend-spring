package org.doogie.liquido.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)
@Table(name = "areas")
public class AreaModel extends BaseModel {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long id;

  // public fields are automatically exposed in REST endpoint
  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String title;

  @NotNull
  @NonNull
  @NotEmpty
  public String description;

  @CreatedBy
  @ManyToOne(fetch = FetchType.EAGER)
  //@JoinColumn
  @NonNull
  @NotNull
  public UserModel createdBy;

  /** always inline createdBy user information as "creator" field. (This is easier as using a full blown spring projection as in IdeaProjection.java) */
  public UserModel getCreator() {
    return this.createdBy;
  }

}
