package org.doogie.liquido.model;

import graphql.Assert;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.doogie.liquido.security.LiquidoAuthUser;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Team with members
 */
@Data
@EqualsAndHashCode(of="teamName", callSuper = true)    // Compare teams by their unique team.name  (and ID)
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)  		// automatically set UpdatedAt and CreatedAt
@Table(name = "teams")
public class TeamModel extends BaseModel {
  /** Name of team. Must be unique over all teams. */
	@NotNull
  @NonNull
  @Column(unique = true)
	@GraphQLQuery(name = "teamName")
  String teamName;

  /** A code that can be sent to other users to invite them into your team. */
	@GraphQLQuery(name = "inviteCode")
  String inviteCode = null;

  /**
	 * Members of this team.
	 * A team must have at least one TEAM_ADMIN, but it may have several admins.
	 */
	@GraphQLQuery(name = "members")
	@OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)  // when a team is loaded also load its members
  Set<UserModel> members = new HashSet<>();

	@GraphQLQuery(name = "polls")
	@OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)  // when a team is loaded also load its polls
	Set<UserModel> polls = new HashSet<>();   //BUGFIX: Changed from List to Set https://stackoverflow.com/questions/4334970/hibernate-throws-multiplebagfetchexception-cannot-simultaneously-fetch-multipl

	/** Create a new Team entity */
	public TeamModel(String teamName, UserModel admin) {
		Assert.assertTrue(admin.roles.contains(LiquidoAuthUser.ROLE_TEAM_ADMIN), "Team needs an admin");
		this.teamName = teamName;
		this.members.add(admin);
		//admin.setTeamId(this.id);  //BUGFIX: this needs to be done manually **after** TeamModel has been saved and thus has an ID.
		this.inviteCode = DigestUtils.md5Hex(teamName).substring(0,6).toUpperCase();
	}

	/**
	 * Get TEAM_ADMIN(s)
	 * @return the admins of this team
	 */
	@GraphQLQuery(name = "admins")
	public Set<UserModel> getAdmins() {
		return this.members.stream()
			.filter(user -> user.roles.contains(LiquidoAuthUser.ROLE_TEAM_ADMIN))
			.collect(Collectors.toSet());
	}

  @Override
  public String toString() {
  	StringBuffer buf = new StringBuffer();
  	Iterator adminsIterator = this.getAdmins().iterator();
    buf.append("TeamModel[");
		buf.append("id=" + id);
		buf.append(", teamName='" + this.teamName + '\'');
		if (adminsIterator.hasNext()) {
			buf.append(", firstAdmin='" + adminsIterator.next().toString() + "'");
		}
		buf.append(", numMembers="+this.members.size());
		buf.append(']');
		return buf.toString();
  }

}
