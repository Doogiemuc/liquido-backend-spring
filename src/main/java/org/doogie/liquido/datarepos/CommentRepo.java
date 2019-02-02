package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.CommentModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Paged REST resource for comments. Comments can also be loaded via {@link org.doogie.liquido.model.CommentProjection}
 */
@RepositoryRestResource(collectionResourceRel = "comments", path = "comments", itemResourceRel = "comment")
public interface CommentRepo extends PagingAndSortingRepository<CommentModel, Long> {

}
