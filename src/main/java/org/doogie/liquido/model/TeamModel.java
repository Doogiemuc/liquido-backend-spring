package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
 * A Team with its admin(s) and members.
 */
@Data
@EqualsAndHashCode(callSuper = true)    // Compare teams by their unique ID. teamName may change
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "teams")
//TODO:  uniqueConstraints= {             // A user may join a team only once, so email must be unique within one team.
//	@UniqueConstraint(columnNames = {"email", ""})
//})
public class TeamModel extends BaseModel {
  /** Name of team. TeamName must be unique over all teams! */
	@NotNull
  @NonNull
  @Column(unique = true)      // teamName must be unique throughout all teams in LIQUIDO
	@GraphQLQuery(name = "teamName")
  String teamName;

  /** A code that can be sent to other users to invite them into your team. */
	@GraphQLQuery(name = "inviteCode")
  String inviteCode = null;

	/**
	 * The initial creator of a team is the first admin.
	 * He may then appoint further admin colleages.
	 */
	@GraphQLQuery(name = "admins")
	@OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)  // when a team is loaded also load its admins
	Set<UserModel> admins = new HashSet<>();

  /**
	 * Members of this team.
	 */
	@GraphQLQuery(name = "members")
	@OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)  // when a team is loaded also load its members
  Set<UserModel> members = new HashSet<>();

	/** The polls in this team */
	//This is the one side of a bidirectional OneToMany relationship. Keep in mind that you then MUST add mappedBy to map the reverse direction.
	//And dont forget the @JsonBackReference on the other side to prevent StackOverflows when serializing a TeamModel
	@GraphQLQuery(name = "polls")
	@OneToMany(mappedBy = "team", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)  // when a team is loaded, then do not immediately also load all polls
	//@JsonManagedReference
	Set<PollModel> polls = new HashSet<>();   //BUGFIX: Changed from List to Set https://stackoverflow.com/questions/4334970/hibernate-throws-multiplebagfetchexception-cannot-simultaneously-fetch-multipl

	/** Create a new Team entity */
	public TeamModel(@NonNull String teamName, @NonNull UserModel admin) {
		this.teamName = teamName;
		this.admins.add(admin);
		this.inviteCode = DigestUtils.md5Hex(teamName).substring(0,6).toUpperCase();
	}

  @Override
  public String toString() {
  	StringBuffer buf = new StringBuffer();
  	UserModel firstAdmin = this.getAdmins().iterator().next();
    buf.append("TeamModel[");
		buf.append("id=" + id);
		buf.append(", teamName='" + this.teamName + '\'');
		buf.append(", firstAdmin='" + firstAdmin + "'");
		buf.append(", numMembers="+this.members.size());
		buf.append(']');
		return buf.toString();
  }

}
