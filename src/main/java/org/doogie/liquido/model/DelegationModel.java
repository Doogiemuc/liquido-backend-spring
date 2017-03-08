package org.doogie.liquido.model;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Delegation from a user to a proxy in a given area.
 * A user can only have none or exactly one proxy per area.
 * One user may be the proxy for several "delegees" in one area.
 * A delegation is always implicitly created by fromUser, since a user may only choose proxy for himself.
 * This entity only consists of three foreign keys.
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)  //BUGFIX:  https://jira.spring.io/browse/DATAREST-884
//@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "delegations", uniqueConstraints= {
  @UniqueConstraint(columnNames = {"area_id", "from_user_id"})
})
//@IdClass(DelegationID.class)    // This way I could also implement the composite primary key
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

  /** the token that the delegee has created (with his password) so that the proxy can vote on behalf of him
  @NonNull
  @NotNull
  public String voterToken;

  TODO: cannot store this here, because there must not be a connection between fromUser and voterToken
  */

  //A delegation is always implicitly created by "fromUser"
}
