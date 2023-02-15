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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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

	// Each team has members and admins.
	// Each user may also be a member (or admin) of other teams.
	// So this is a @ManyToMany relationship
	// But each user may only be member (or admin) once in this team!

	/**
	 * The initial creator of a team is the first admin.
	 * He may then appoint further admin colleagues.
	 */
	@GraphQLQuery(name = "admins")
	@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)  // when a team is loaded also load its admins
	Set<UserModel> admins = new HashSet<>();

  /**
	 * Members of this team.
	 */
	@GraphQLQuery(name = "members")
	@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)  // when a team is loaded also load its members
  Set<UserModel> members = new HashSet<>();

	/** The polls in this team */
	//This is the one side of a bidirectional OneToMany relationship. Keep in mind that you then MUST add mappedBy to map the reverse direction.
	//And don't forget the @JsonBackReference on the many-side of the relation (in PollModel) to prevent StackOverflowException when serializing a TeamModel
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

	public boolean isAdmin(UserModel admin) {
		return this.admins.contains(admin);
	}
	public boolean emailIsAdmin(String email) {
		return this.admins.stream().anyMatch(admin -> admin.email != null && admin.email.equalsIgnoreCase(email));
	}
	public boolean mobilephoneIsAdmin(String mobilephone) {
		return this.admins.stream().anyMatch(admin -> admin.mobilephone != null && admin.mobilephone.equals(mobilephone));
	}

	public boolean isMember(UserModel member) {
		return this.members.contains(member);
	}
	public boolean emailIsMember(String email) {
		return this.members.stream().anyMatch(member -> member.email != null && member.email.equalsIgnoreCase(email));
	}
	public boolean mobilephoneIsMember(String mobilephone) {
		return this.members.stream().anyMatch(member -> member.mobilephone != null && member.mobilephone.equals(mobilephone));
	}

	/**
	 * Check if a user or admin with that email exists and return it
	 * @param email email of a user or admin in this team
	 * @return the user or admin if it is part of this team
	 */
	public Optional<UserModel> getAdminOrMemberByEmail(String email) {
		return Stream.concat(this.getAdmins().stream(), this.getMembers().stream()).filter(u -> u.email.equalsIgnoreCase(email)).findFirst();
	}

  @Override
  public String toString() {
  	StringBuffer buf = new StringBuffer();
  	UserModel firstAdmin = this.getAdmins().iterator().next();
    buf.append("TeamModel[");
		buf.append("id=" + id);
		buf.append(", teamName='" + this.teamName + '\'');
		buf.append(", firstAdmin='" + firstAdmin + "'");
		buf.append(", numAdminAndMembers=" + (this.admins.size() + this.members.size()));
		buf.append(']');
		return buf.toString();
  }



}
