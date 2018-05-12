package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.CommentModel;
import org.doogie.liquido.model.CommentProjection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "comments", path = "comments", itemResourceRel = "comment")
public interface CommentRepo extends CrudRepository<CommentModel, Long> {
  // just a plain default repository for comments
}
