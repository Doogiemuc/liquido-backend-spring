package org.doogie.liquido.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * One time token that is used for login without a password.
 * A OTT allows a <b>user</b> to login once with this token. After that
 * the token is deleted. Each OTT has a limited time to live (TTL).
 * If a user is member of multiple teams, then he can choose into which team to login.
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OneTimeToken extends BaseModel {
	@NonNull
	@NotNull
	String token;

	@NonNull
	@NotNull
	@OneToOne
	UserModel user;

	@NonNull
	@NotNull
	LocalDateTime validUntil;
}
