package org.doogie.liquido.model;

import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;

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

  /** Members of this team. The first member of the team is its admin. */
	@GraphQLQuery(name = "members")
	@OneToMany(cascade = CascadeType.PERSIST)
  List<UserModel> members = new ArrayList<>();

	/** Create a new Team entity */
	public TeamModel(String teamName, UserModel admin) {
		this.teamName = teamName;
		this.members.add(admin);
		this.inviteCode = DigestUtils.md5Hex(teamName).substring(0,6).toUpperCase();
	}

	/**
	 * First member of team is admin
	 * @return the admin user of this team
	 */
	@GraphQLQuery(name = "admin")
	public UserModel getAdmin() {
		return this.members.get(0);
	}


  @Override
  public String toString() {
  	StringBuffer buf = new StringBuffer();
    buf.append("TeamModel[");
		buf.append("id=" + id);
		buf.append(", teamName='" + this.teamName + '\'');
		buf.append(", adminEmail='" + this.getAdmin().email + "'");
		buf.append(", numMembers="+this.members.size());
		buf.append(']');
		return buf.toString();
  }

}
