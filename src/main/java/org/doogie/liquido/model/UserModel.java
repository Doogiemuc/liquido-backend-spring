package org.doogie.liquido.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * One user / voter / citizen / member of a team
 * When a user creates a new team, then he becomes the admin of that team.
 * A user may also join other teams. Then he is a member in those teams.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)  		    // Let spring automatically set UpdatedAt and CreatedAt
@Table(name = "users")
public class UserModel extends BaseModel {
	/**
	 * User's email adress. This email must be unique within the team.
	 * A user may be registered with the same email in <em>different</em> teams.
	 */
	@NotNull
  @NonNull
  public String email;

	/** User's mobile phone number. Needed for login via SMS code */
	//@Column(unique = true)  Are you really sure that every user have their own mobile phone? Or do some people share their mobilephone? Think worldwide!
	String mobilephone;

	/**
	 * www.twilio.com Authy user id for 2FA authentication.
	 * NO PASSWORD!  Passwords are soooo old fashioned :-)
	 */
	public long authyId;


	// Implementation note:
	// A UserModel does not contain a reference to a team. Only the TeamModel has members and admins. (Helps a lot with preventing JsonBackReferences)
	// One user may be admin or member of several teams!


	/** Username, Nickname */
	@NotNull
	@NonNull
	String name;

	/** (optional) User's website or bio or social media profile link */
	String website = null;

	/** Avatar picture URL */
	String picture = null;


	/** timestamp of last login */
	LocalDateTime lastLogin;

	public UserModel(@NonNull String email, @NonNull String name, String mobilephone, String website, String picture) {
		//TODO: mobilephone is necessary for Authy.   if (mobilephone == null || mobilephone.trim().length() == 0) throw new IllegalArgumentException("Need mobilephone to create a UserModel");
		this.email = email;
		this.name = name;
		this.mobilephone = mobilephone;
		this.website = website;
		this.picture = picture;
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
