package org.doogie.liquido.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

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

  //NO PASSWORD!  Passwords are soooo old fashioned :-)

	/**
	 * www.twilio.com Authy user id for authentication
	 */
	public long authyId;

	@Embedded
	public UserProfileModel profile;

	/** timestamp of last login */
	LocalDateTime lastLogin;

	public UserModel(@NotNull String email, @NotNull String name, String mobilephone, String website, String picture) {
		if (email == null || email.length() == 0) throw new IllegalArgumentException("Need an email to create a UserModel");
		if (mobilephone == null || mobilephone.length() == 0) throw new IllegalArgumentException("Need mobilephone to create a UserModel");
		this.email = email;
		this.profile = new UserProfileModel();
		this.profile.setName(name);
		this.profile.setMobilephone(mobilephone);
		this.profile.setWebsite(website);
		this.profile.setPicture(picture);
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
