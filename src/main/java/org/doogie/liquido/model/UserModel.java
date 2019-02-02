package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * One user / voter / citizen
 */
@Data
@EqualsAndHashCode(of="email", callSuper = true)    // Compare users by their uniquie e-mail  (and ID)
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "users")
public class UserModel extends BaseModel {
  @NotNull
  @NonNull
  @Column(unique = true)
  public String email;

  //NO PASSWORD!  Passwords are soooo old fashioned :-)

	@Embedded
	public UserProfileModel profile;

	/** timestamp of last login */
	LocalDateTime lastLogin;

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
