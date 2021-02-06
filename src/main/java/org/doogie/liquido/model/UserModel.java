package org.doogie.liquido.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.doogie.liquido.security.LiquidoAuthUser;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * One user / voter / citizen / member of a team
 * This class is also used as HTTP Principal in spring-security. So it needs to be lightweight
 */
@Data
@EqualsAndHashCode(of="id", callSuper = true)    // Compare users by their unique ID  (email may appear in several teams)
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)  		// automatically set UpdatedAt and CreatedAt
@Table(name = "users", uniqueConstraints= {
	@UniqueConstraint(columnNames = {"email", "teamId"})  // Email must be unique within one team.
})
public class UserModel extends BaseModel {
	/**
	 * User's email adress. This email must be unique within the team.
	 * A user may be registered with the same email in <em>different</em> teams.
	 */
	@NotNull
  @NonNull
  public String email;

	/**
	 * www.twilio.com Authy user id for 2FA authentication.
	 * NO PASSWORD!  Passwords are soooo old fashioned :-)
	 */
	public long authyId;

	/**
	 * Link to the team that the user is a member (or admin) of.
	 * The TeamModel is not directly referenced here, because our Liquido UserModel
	 * is also used as the HTTP Principal in spring-security. So it needs to be lightweight.
	 * When the team data (with polls, etc) is needed then it must be loaded manually via the teamRepo.
 	 */
	public Long teamId;  //TODO: One user may be a member in several teams.  N:M @ManyToMany(mappedby="TEAM_MEMBERS")

	/*  Decided to only reference the teamId.
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	public TeamModel team;
	*/

	/**
	 * Every user implicitly has {@link LiquidoAuthUser#ROLE_USER}. (This does not need to be stored in the DB. Its added by default.)
	 * Admins will also have {@link LiquidoAuthUser#ROLE_TEAM_ADMIN} here.
	 * (More roles by be added in future versions of LIQUIDO.)
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name="USER_ROLES")
	public Set<String> roles = new HashSet<>();

	/** Username, Nickname */
	@NotNull
	@NonNull
	String name;

	/** (optional) User's website or bio or social media profile link */
	@Nullable
	String website = null;

	/** Avatar picture URL */
	@Nullable
	String picture = null;

	/** User's mobile phone number. Needed for login via SMS code */
	//@Column(unique = true)  Are you really sure that every user have their own mobile phone? Or do some people share their mobilephone? Think worldwide!
	String mobilephone;

	/** timestamp of last login */
	LocalDateTime lastLogin;

	public UserModel(@NotNull String email, @NotNull String name, String mobilephone, String website, String picture) {
		if (email == null || email.length() == 0) throw new IllegalArgumentException("Need an email to create a UserModel");
		this.email = email;
		this.name = name;
		this.mobilephone = mobilephone;
		this.website = website;
		this.picture = picture;
		this.roles.add(LiquidoAuthUser.ROLE_USER);
	}

	/**
	 * Create an admin of a team. You <b>MUST</b> then manually call <pre>admin.setTeamId(team.id)</pre>!
	 * @return the newly created admin UserModel
	 */
	public static UserModel asTeamAdmin(@NotNull String email, @NotNull String name) {
		UserModel admin = new UserModel(email, name, null, null, null);
		admin.roles.add(LiquidoAuthUser.ROLE_TEAM_ADMIN);
		return admin;
	}

  @Override
  public String toString() {
  	StringBuffer buf = new StringBuffer();
    buf.append("UserModel[");
		buf.append("id=" + id);
		buf.append(", email='" + email + '\'');
		buf.append(", name='" + name + '\'');
		buf.append(", mobilephone=" + mobilephone);
		buf.append(", picture=" + picture);
		buf.append(']');
		return buf.toString();
  }

  public String toStringShort() {
		StringBuffer buf = new StringBuffer();
		buf.append("UserModel[");
		buf.append("id=" + id);
		buf.append(", email='" + email + '\'');
		buf.append(']');
		return buf.toString();
	}

}
