package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.PollModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Database abstraction layer for "polls".
 */
@RepositoryRestResource(collectionResourceRel = "polls", path = "polls", itemResourceRel = "poll") //, excerptProjection = LawProjection.class)
public interface PollRepo extends CrudRepository<PollModel, Long> {

  List<PollModel> findByStatus(@Param("status") PollModel.PollStatus status);

  /* OBSOLETE. Replaced by PollModel.getProposals()
   * Find competing proposals
   * @param proposal any proposal (not nedessarily the iniital one)
   * @return the list of alternative/competing proposals
   *
  @Query("select l from LawModel l where l.initialLaw = :#{#proposal.initialLaw} order by l.createdAt")   //see https://spring.io/blog/2014/07/15/spel-support-in-spring-data-jpa-query-definitions
  List<LawModel> findCompeting(@Param("proposal") LawModel proposal);
  */
}
