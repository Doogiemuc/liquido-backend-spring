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

  /**
   * Find recent polls in that status in that area
   * @param status PollModel.PollStatus
   * @param area an area
   * @return list of polls
   */
  @Query("SELECT DISTINCT poll FROM PollModel poll JOIN LawModel prop on prop.poll = poll WHERE poll.status = :status and prop.area = :area order by poll.createdAt desc")
  List<PollModel> findByStatusAndArea(@Param("status") PollModel.PollStatus status, @Param("area")AreaModel area);

  /**
   * find all polls in one area
   * @param area an area
   * @return list of polls in that rea
   */
  @Query("SELECT DISTINCT poll FROM PollModel poll JOIN LawModel prop on prop.poll = poll WHERE prop.area = :area order by poll.createdAt desc")
  List<PollModel> findByArea(@Param("area")AreaModel area);


  // the /polls  endpoint is READ-ONLY!!  To builder a poll one must use the PollRestController
  //TODO: set @RepositoryRestResource(exported=false) and the only export  specific read-only methods!

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