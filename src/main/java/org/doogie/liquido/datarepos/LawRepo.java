package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Database abstraction for MongoDB collection "laws".
 */
@RepositoryRestResource(collectionResourceRel = "laws", path = "laws", itemResourceRel = "law")
public interface LawRepo extends CrudRepository<LawModel, Long> {

  LawModel findByTitle(String title);

  List<LawModel> findByStatus(LawModel.LawStatus status);

  List<LawModel> findByInitialLaw(LawModel initialLaw);
}
