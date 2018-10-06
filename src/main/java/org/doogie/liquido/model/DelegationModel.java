package org.doogie.liquido.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

/**
 * Delegation from a user to a proxy in a given area.
 * A user can only have none or exactly one proxy per area.
 * One user may be the proxy for several "delegees" in one area.
 * A delegation is always implicitly created by fromUser, since a user may only choose proxy for himself.
 * This entity only consists of three foreign keys.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor  					//see also:  https://jira.spring.io/browse/DATAREST-884
@Table(name = "delegations", uniqueConstraints= {
  @UniqueConstraint(columnNames = {"area_id", "from_user_id"})  // A user may only assign one proxy per area!
})
//@IdClass(DelegationID.class)    //MAYBE: composite primary key.  But has issues with spring data rest: How to post composite IDs
public class DelegationModel extends BaseModel {

	/** Area that this delegation is in */
	@NonNull
	@NotNull
	@OneToOne
	public AreaModel area;

	/** Voter that delegated his right to vote to a proxy */
	@NonNull
	@NotNull
	@OneToOne
	public UserModel fromUser;

  /** Proxy that receives the delegation and can now cast votes in place of the voter */
  @NonNull
  @NotNull
  @OneToOne
  public UserModel toProxy;

  // Implementation notes:
  // - A delegation is always implicitly created by "fromUser"

}
