package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Delegation from a user to a proxy in a given area.
 * This entity only consists of the three foreign keys.
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "delegations")
public class DelegationModel extends BaseModel {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long id;

  /** Area that this delegation is in */
  @NonNull
  @NotNull
  @ManyToOne
  public AreaModel area;

  /** reference to delegee that delegated his vote */
  @NonNull
  @NotNull
  @ManyToOne
  public UserModel fromUser;

  /** reference to proxy that receives the delegation */
  @NonNull
  @NotNull
  @ManyToOne
  public UserModel toProxy;

  //A delegation is always implicitly created by "fromUser"
}
