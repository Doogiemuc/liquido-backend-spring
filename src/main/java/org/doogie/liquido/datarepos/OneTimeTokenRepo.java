package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.OneTimeToken;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

// not exposed via REST !
public interface OneTimeTokenRepo extends CrudRepository<OneTimeToken, Long> {
	OneTimeToken findByToken(String token);

	List<OneTimeToken> findByUserEmail(String email);
}
