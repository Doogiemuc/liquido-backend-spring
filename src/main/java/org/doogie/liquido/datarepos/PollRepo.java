package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.rest.PollRestController;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

/**
 * <b></b>Read-only</b> database abstraction layer for "polls".
 * Polls must not be created or edited through this @RepositoryRestResource. Instead use our custom {@link PollRestController}
 */
@RepositoryRestResource(collectionResourceRel = "polls", path = "polls", itemResourceRel = "poll")
public interface PollRepo extends CrudRepository<PollModel, Long> {

  List<PollModel> findByStatus(@Param("status") PollModel.PollStatus status);


  // the /polls  endpoint is read-only.  To create a poll one must use the PollRestController

  @RestResource(exported = false)
  PollModel save(PollModel pollModel);

  @RestResource(exported = false)
  void delete(Long id);

  @RestResource(exported = false)
  void delete(PollModel ballot);

  @RestResource(exported = false)
  void deleteAll();

}