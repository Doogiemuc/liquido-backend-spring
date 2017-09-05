package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.LawProjection;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

/**
 * This Spring-data repository is a database abstraction layer for "laws".
 */
@RepositoryRestResource(collectionResourceRel = "laws", path = "laws", itemResourceRel = "law", excerptProjection = LawProjection.class)
public interface LawRepo extends CrudRepository<LawModel, Long>  // , LawRepoCustom

  //MAYBE: Check this out:     extends QueryDslPredicateExecutor<LawModel>
  // https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#core.extensions
  // http://www.querydsl.com/

{

  LawModel findByTitle(@Param("title") String title);   // title is unique!

  List<LawModel> findByStatus(@Param("status") LawModel.LawStatus status);

  @Query("select l from LawModel l where l.status = 0")
  List<LawModel> findAllIdeas();

  @Query("select l from LawModel l where l.status = 0 order by l.createdAt desc")
  List<LawModel> recentIdeas();

  /**
   * Find laws in a given status, e.g. proposal that were recently supported by this user.
   * (This spring habbit of generating query from method names is frightening black magic. But it does actually work quite well.
   */
  @RestResource(path = "findSupported")
  List<LawModel> findDistinctByStatusAndSupportersContains(@Param("status") LawModel.LawStatus status, @Param("user") UserModel user);

  //@RestResource(path = "recentIdeas")
  //List<LawModel> findFirst10ByOrderByCreatedAtDesc();  // http://docs.spring.io/spring-data/data-mongo/docs/1.9.6.RELEASE/reference/html/#repositories.limit-query-result

  /*
  List<LawModel> findByInitialLaw(@Param("initialLaw") LawModel initialLaw);

  /* @return the number of competing laws
  Long countByInitialLaw(@Param("initialLaw") LawModel initialLaw);

  /**
   * Find competing proposals
   * @param proposal any proposal (not necessarily the iniital one)
   * @return the list of alternative/competing proposals
   *
  @Query("select l from LawModel l where l.initialLaw = :#{#proposal.initialLaw} order by l.createdAt")   //see https://spring.io/blog/2014/07/15/spel-support-in-spring-data-jpa-query-definitions
  List<LawModel> findCompeting(@Param("proposal") LawModel proposal);

  */
}
