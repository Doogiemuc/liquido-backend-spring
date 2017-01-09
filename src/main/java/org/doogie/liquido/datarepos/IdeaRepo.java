package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.IdeaProjection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

/**
 * Database abstraction "ideas".
 * Every idea will automatically have its createdBy user inlined.
 */
@RepositoryRestResource(collectionResourceRel = "ideas", path = "ideas", itemResourceRel = "idea", excerptProjection = IdeaProjection.class)
public interface IdeaRepo extends CrudRepository<IdeaModel, Long> {

  //TODO: make Ideas.getAll() pageable?  need to be in sync with client implementation

  IdeaModel findByTitle(@Param("title") String title);

  /**
   * Find the most recently created ideas.
   * published as  /liquido/v2/ideas/search/recentIdeas
   */
  @RestResource(path = "recentIdeas")
  List<IdeaModel> findFirst10ByOrderByCreatedAtDesc();  // http://docs.spring.io/spring-data/data-mongo/docs/1.9.6.RELEASE/reference/html/#repositories.limit-query-result

}
