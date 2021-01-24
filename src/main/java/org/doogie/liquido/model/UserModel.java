package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.doogie.liquido.security.LiquidoAuthUser;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * One user / voter / citizen
 */
@Data
@EqualsAndHashCode(of="email", callSuper = true)    // Compare users by their unique e-mail  (and ID)
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)  		// automatically set UpdatedAt and CreatedAt
@Table(name = "users")
public class UserModel extends BaseModel {
  @NotNull
  @NonNull
  @Column(unique = true)
  public String email;

	/**
	 * www.twilio.com Authy user id for authentication
	 * NO PASSWORD!  Passwords are soooo old fashioned :-)
	 */
	public long authyId;

	/**
	 * Link to the team that the user is a member (or admin) of.
	 * The TeamModel is not directly referenced here, because our Liquido UserModel
	 * is also used as the HTTP Principal in spring-security. So it needs to be lightweight.
	 * When the team data (with polls, etc) is needed then it must be loaded manually via the teamRepo.
 	 */
	public Long teamId;

	/*
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

	@Embedded
	public UserProfileModel profile;

	/** timestamp of last login */
	LocalDateTime lastLogin;

	public UserModel(@NotNull String email, @NotNull String name, String mobilephone, String website, String picture) {
		if (email == null || email.length() == 0) throw new IllegalArgumentException("Need an email to create a UserModel");
		this.email = email;
		this.profile = new UserProfileModel();
		this.profile.setName(name);
		this.profile.setMobilephone(mobilephone);
		this.profile.setWebsite(website);
		this.profile.setPicture(picture);
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
		if (this.getProfile() != null) {
			buf.append(", profile.mobilephone=" + this.getProfile().getMobilephone());
			buf.append(", profile.picture=" + this.getProfile().getPicture());
		}
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
