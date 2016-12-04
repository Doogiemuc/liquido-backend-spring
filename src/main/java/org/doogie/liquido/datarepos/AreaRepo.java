package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "areas", path = "areas")
public interface AreaRepo extends MongoRepository<AreaModel, String> {

  /**
   * find an area by its unique title
   * @param title title of the area
   * @return one AreaModel or null if no area with that title was found
   */
  AreaModel findByTitle(@Param("title")String title);

}
