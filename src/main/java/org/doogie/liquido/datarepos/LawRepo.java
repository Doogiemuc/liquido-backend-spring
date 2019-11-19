package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.LawProjection;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;
import java.util.Optional;

// https://docs.spring.io/spring-data/rest/docs/current/reference/html/
//DONE:   implement queries with offset+count param: https://gist.github.com/tcollins/0ebd1dfa78028ecdef0b


/**
 * This Spring-data repository is a database abstraction layer for "laws".
 */
@RepositoryRestResource(collectionResourceRel = "laws", path = "laws", itemResourceRel = "proposal", excerptProjection = LawProjection.class)
public interface LawRepo extends PagingAndSortingRepository<LawModel, Long>
    //, LawRepoCustom
    , JpaSpecificationExecutor<LawModel>
    //, QueryByExampleExecutor<LawModel>
    //, QuerydslPredicateExecutor<LawModel>

{

  Optional<LawModel> findByTitle(@Param("title") String title);   // title is unique!

  /** can for example be used to find all with status=IDEA. Supports paging */
  Page<LawModel> findByStatus(@Param("status") LawModel.LawStatus status, Pageable p);

  /**
   * find recently created ideas
   * http://localhost:8080/liquido/v2/laws/search/recentIdeas?page=1&size=100
   * @return list of ideas ordered by date created descending, newest first
   */
  @Query("select l from LawModel l where l.status = 0 order by l.createdAt desc")
  Page<LawModel> recentIdeas(Pageable p);

  /**
   * case insensitive full-text search for ideas, proposals or laws in their fields title, description, name or email of creator
   * @param searchterm text that needs to be contained
   * @param status <b>optionally</b> filter for status == IDEA|PROPOSAL|LAW   (must be uppercase!)
   * @param p limit of one page and page number
   * @return matching ideas, proposals and laws
   */
  @Deprecated   // This has been superseded by LawService#findBySearchQuery()
  @Query("select distinct l from LawModel l where " +
      "(:status is null OR l.status = :status) " +
      "AND (" +
      "UPPER(l.title) like CONCAT('%', UPPER(:s), '%') OR " +
      "UPPER(l.description) like CONCAT('%', UPPER(:s), '%') OR " +
      "UPPER(l.createdBy.email) like CONCAT('%', UPPER(:s), '%') OR " +
      "UPPER(l.createdBy.profile.name) like CONCAT('%', UPPER(:s), '%') " +
      ")")
  Page<LawModel> fulltextSearch(@Param("s") String searchterm, @Param("status") LawModel.LawStatus status, Pageable p);

  //Or the spring data way, but this cannot search in multiple fields
  //Page<LawModel> findByTitleContainingIgnoreCase(@Param("s") String searchterm, Pageable p);

  /**
   * Query for proposals that reached their quorum since a given date<br/>
   * Usage: <pre>/laws/search/reachedQuorumSince?since=2017-09-01</pre>
   *
   * @param since date how long ago. Format of URL parameter: <pre>?since=yyyy-MM-dd</pre>
   * @return list of proposals that recently reached their quorum.  Most recent one is first in list. List may be empty.
   */
  @Query("select l from LawModel l where l.status = 1 and reachedQuorumAt > :since order by reachedQuorumAt desc")
  List<LawModel> reachedQuorumSince(@DateTimeFormat(pattern = "yyyy-MM-dd") @Param("since") Date since);

  /**
   * Query for proposals that reach their quorum since a given date <b>and</b> that were created by a given user.
   * @param since date how long ago. Format of URL parameter: <pre>?since=yyyy-MM-dd</pre>
   * @param createdBy userURI that created the proposal: <pre>/users/4711</pre>
   * @return list of proposals that match the query
   */
  @RestResource(path = "reachedQuorumSinceAndCreatedBy")
  List<LawModel> findByReachedQuorumAtGreaterThanEqualAndCreatedBy(@DateTimeFormat(pattern = "yyyy-MM-dd") @Param("since") Date since, @Param("createdBy") UserModel createdBy);

  /**
   * Find ideas or proposals that were created by a given user
   * @param status idea, proposal or proposal
   * @param user a user that created them
   * @return list of LawModels that were created by this user
   */
  @RestResource(path = "findByStatusAndCreator")
  List<LawModel> findDistinctByStatusAndCreatedBy(@Param("status") LawModel.LawStatus status, @Param("user") UserModel user);

  /**
   * Find ideas, proposals or laws that are supported by a given user.
   * @param status  pass as number
   * @param user the supporter
   * @return a list of LawModels
   * (This spring habit of generating query from method names is frightening black magic. But it does actually work quite well.
   */
  @RestResource(path = "findSupportedBy")
  List<LawModel> findDistinctByStatusAndSupportersContains(@Param("status") LawModel.LawStatus status, @Param("user") UserModel user);


  //@Query("SELECT l FROM LawModel l JOIN CommentModel c ON c.  WHERE l.status = 1 ")
  //List<LawModel> getRecentlyDiscussed();
}
