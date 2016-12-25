package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Database abstraction for MongoDB collection "laws".
 */
@RepositoryRestResource(collectionResourceRel = "laws", path = "laws", itemResourceRel = "law")
public interface LawRepo extends MongoRepository<LawModel, String> {
  // EMPTY: all CRUD operations are automatically created.

}
