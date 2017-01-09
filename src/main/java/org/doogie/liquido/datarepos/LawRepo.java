package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.LawProjection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Database abstraction for MongoDB collection "laws".
 */
@RepositoryRestResource(collectionResourceRel = "laws", path = "laws", itemResourceRel = "law", excerptProjection = LawProjection.class)
public interface LawRepo extends CrudRepository<LawModel, Long> {

  LawModel findByTitle(@Param("title") String title);   // title is unique!

  List<LawModel> findByStatus(@Param("status") LawModel.LawStatus status);

  List<LawModel> findByInitialLaw(@Param("initialLaw") LawModel initialLaw);
}
