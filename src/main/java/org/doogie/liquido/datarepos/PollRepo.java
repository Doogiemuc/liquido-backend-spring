package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.rest.PollRestController;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * <b></b>Read-only</b> database abstraction layer for "polls".
 * This RepositoryRestResouce is <b>read-only!</b>
 * Polls must not be created or edited through this @RepositoryRestResource. Instead use our custom {@link PollRestController}
 */
@RepositoryRestResource(collectionResourceRel = "polls", path = "polls", itemResourceRel = "poll")
public interface PollRepo extends CrudRepository<PollModel, Long> {

  List<PollModel> findByStatus(@Param("status") PollModel.PollStatus status);

  @Query("SELECT DISTINCT poll FROM PollModel poll JOIN poll.proposals prop WHERE poll.status = :status and prop.area = :area order by reachedQuorumAt desc")
  List<PollModel> findByStatusAndArea(@Param("status") PollModel.PollStatus status, @Param("area")AreaModel area);

  // the /polls  endpoint is READ-ONLY!!  To builder a poll one must use the PollRestController

  @RestResource(exported = false)
  PollModel save(PollModel pollModel);

  @RestResource(exported = false)
  <S extends PollModel> Iterable<S> saveAll(Iterable<S> polls);

  @RestResource(exported = false)
  void delete(PollModel poll);

  // Before you can delete a poll, you must unlink its proposals and delete its ballots first!
  @RestResource(exported = false)
  void deleteById(Long id);

  @RestResource(exported = false)
  void deleteAll();

  @RestResource(exported = false)
  void deleteAll(Iterable<? extends PollModel> var1);

}