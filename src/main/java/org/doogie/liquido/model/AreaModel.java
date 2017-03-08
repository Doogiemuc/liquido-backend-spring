package org.doogie.liquido.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(of = {"id", "title"}, callSuper = false)
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

  /* This way one could inline createdBy user information as "creator" field.  But this would unconditionally ALWAYS inline that informatin.  e.g. also  as law.area.creator  in list of laws which I do not want
  public UserModel getCreator() {
    return this.createdBy;
  }
   */

}
