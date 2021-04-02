package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.OneTimeToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

// not exposed via REST !
public interface OneTimeTokenRepo extends CrudRepository<OneTimeToken, Long> {
	Optional<OneTimeToken> findByToken(String token);

	List<OneTimeToken> findByUserEmail(String email);
}
