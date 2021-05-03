package org.doogie.liquido.model;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.types.GraphQLType;
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
//@GraphQLType(name="user", description = "A LiquidoUser that can be an admin or member in a team.")  // well be named "userInput" by graphql-spqr
public class UserModel extends BaseModel {
	/*
	  About the equality of UserModels

	  Equality is a big thing in Java! :-) And it is an especially nasty issue for UserModels.
	  When are two user models "equal" to each other?
	   - then their ID matches?
	   - when they have the same email address? (this is what most people would assume at first.)
	   - when they have the same mobile phone? (security relevant for login via SMS token!)
	   - do the other attributes also need to match (deep equals)
	  Always distinguish between a "user" and a "human being"! A human might register multiple times with independent email addresses!
	  There is no way an app could ever prevent this (other then requiring DNA tests).

	  LIQUIDO UserModels equal when their ID matches. This should normally always imply that all the other attributes also match.
	  If not, we've been hacked!
	*/

	/**
	 * User's email adress. This email must be unique within the team.
	 * A user may be registered with the same email in <em>different</em> teams.
	 */
	@NotNull
  @NonNull
  public String email;

	/**
	 * User's mobile phone number. Needed for login via SMS code.
	 * Mobilephone numbers in the DB are cleaned first: See cleanMobilePhone()
	 */
	//@Column(unique = true)  Are you really sure that every user have their own mobile phone? Or do some people share their mobilephone? Think worldwide!
	//TODO: make this required
	public String mobilephone;

	/**
	 * www.twilio.com Authy user id for 2FA authentication.
	 * NO PASSWORD!  Passwords are soooo old fashioned :-)
	 */
	@GraphQLIgnore
	public long authyId;

	/** Last team the user was logged in. This is used when user is member of multiple teams. */
	@GraphQLIgnore
	public long lastTeamId = -1;


	// Implementation note:
	// A UserModel does not contain a reference to a team. Only the TeamModel has members and admins. (Helps a lot with preventing JsonBackReferences)
	// One user may be admin or member of several teams!


	/** Username, Nickname */
	@NotNull
	@NonNull
	@GraphQLInputField
	public String name;

	/** (optional) User's website or bio or social media profile link */
	public String website = null;

	/** Avatar picture URL */
	public String picture = null;


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
