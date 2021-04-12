package org.doogie.liquido.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * One time token that is used for login without a password.
 * A OTT allows a user to login <b>once</b> with this token. After that
 * the token will be deleted. Each OTT has a limited time to live (TTL).
 */
@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OneTimeToken extends BaseModel {
	/** Nonce of the token. Can for exmaple be a UUID. */
	@NonNull
	@NotNull
	String nonce;

	/** LIQUIDO User that this token belongs to. Only this user is allowed to use this token.*/
	@NonNull
	@NotNull
	@OneToOne
	UserModel user;

	/** Expiry date of token. After this date the token is not valid anymore. */
	@NonNull
	@NotNull
	LocalDateTime validUntil;
}
