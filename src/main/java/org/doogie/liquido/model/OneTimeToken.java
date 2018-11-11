package org.doogie.liquido.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * One time login token. Either from SMS (4 digits) or from login email
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


	public enum TOKEN_TYPE {
		EMAIL,
		SMS
	}
	@NonNull
	@NotNull
	TOKEN_TYPE token_type;

	@NonNull
	@NotNull
	LocalDateTime validUntil;
}