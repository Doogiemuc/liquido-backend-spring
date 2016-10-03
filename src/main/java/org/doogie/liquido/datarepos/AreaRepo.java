package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AreaRepo extends MongoRepository<AreaModel, String> {

  AreaModel findByTitle(String title);

}
