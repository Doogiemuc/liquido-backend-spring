package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.KeyValueModel;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring repository for storing arbitrary key=value pairs.
 * Can be used for internal configuration values.
 * This repository is *not* exposed as RestResource!
 */
public interface KeyValueRepo extends CrudRepository<KeyValueModel, Long> {
  // EMTPY
}
