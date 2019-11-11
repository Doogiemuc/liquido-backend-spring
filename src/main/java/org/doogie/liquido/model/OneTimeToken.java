package org.doogie.liquido.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * One time token that is used for login via SMS or EMail.
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
	TOKEN_TYPE tokenType;

	@NonNull
	@NotNull
	LocalDateTime validUntil;
}
