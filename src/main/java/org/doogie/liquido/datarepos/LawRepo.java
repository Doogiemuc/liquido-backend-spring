package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.LawProjection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * This Spring-data repository is a database abstraction layer for "laws".
 */
@RepositoryRestResource(collectionResourceRel = "laws", path = "laws", itemResourceRel = "law", excerptProjection = LawProjection.class)
public interface LawRepo extends CrudRepository<LawModel, Long> {

  LawModel findByTitle(@Param("title") String title);   // title is unique!

  List<LawModel> findByStatus(@Param("status") LawModel.LawStatus status);

  /*
  List<LawModel> findByInitialLaw(@Param("initialLaw") LawModel initialLaw);

  /* @return the number of competing laws
  Long countByInitialLaw(@Param("initialLaw") LawModel initialLaw);

  /**
   * Find competing proposals
   * @param proposal any proposal (not nedessarily the iniital one)
   * @return the list of alternative/competing proposals
   *
  @Query("select l from LawModel l where l.initialLaw = :#{#proposal.initialLaw} order by l.createdAt")   //see https://spring.io/blog/2014/07/15/spel-support-in-spring-data-jpa-query-definitions
  List<LawModel> findCompeting(@Param("proposal") LawModel proposal);

  */
}
