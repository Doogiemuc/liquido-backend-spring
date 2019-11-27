package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.data.LiquidoProperties;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.CommentModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.LawQuery;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods for a Law. These are for example used by {@link org.doogie.liquido.model.LawProjection}
 * And advanced search for LawModels by filter query.
 */
@Slf4j
@Service
public class LawService {

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  CommentRepo commentRepo;

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  LiquidoProperties prop;


  /**
   * Check if a given idea is already supported by the currently logged in user.
   * Remark: The creator is not counted as a supporter! Of course one could say that an idea is implicitly supported by its creator. But that is not counted in the list of supporters,
   * because an idea needs "external" supporters, to become a proposal for a proposal.
   * @param law a LawModel
   * @return true  IF this idea is supported by the currently logged in user
   *         false IF there is no user logged in
   */
  public boolean isSupportedByCurrentUser(LawModel law) {
    //this is used in LawProjection.java
    Optional<UserModel> currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
    if (!currentlyLoggedInUser.isPresent()) return false;
    return law.getSupporters().contains(currentlyLoggedInUser.get());
  }

/**
 * Add a supporter to an idea or proposal.
 * This is actually a quite interesting algorithm, because the initial creator of the idea must not be added as supporter
 * and supporters must not be added twice.
 * @param supporter the user that 'likes' this idea and wants to support and discuss it.
 * @param idea the idea to add to
 * @return the saved idea
 */
  public LawModel addSupporter(@NotNull UserModel supporter, @NotNull LawModel idea) throws LiquidoException {
    if (idea.getCreatedBy().equals(supporter)) throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_SUPPORTER, "You cannot support your own idea.");
    if (idea.getSupporters().contains(supporter)) return idea;  // If user already supports this idea, then return idea as is.
    log.info("addSupporter: "+supporter.toStringShort()+" now supports "+idea);
    idea.getSupporters().add(supporter);
    idea = lawRepo.save(idea);
    idea = checkQuorum(idea);
    return idea;
  }

  /**
   * When an idea reaches its quorum, then it becomes a proposal.
   * This is automatically called from {@link LawEventHandler} when supporter has been added via REST: POST /laws/4711/supporters
   * @param idea an idea where a supporter has been added.
   */
  public LawModel checkQuorum(LawModel idea) {
    if (idea != null &&
        idea.getStatus().equals(LawModel.LawStatus.IDEA) &&
        idea.getNumSupporters() >= prop.supportersForProposal) {
      log.info("Idea (id="+idea.getId()+") '"+idea.getTitle()+"' reached its quorum with "+idea.getNumSupporters()+" supporters.");
      idea.setStatus(LawModel.LawStatus.PROPOSAL);
      idea.setReachedQuorumAt(new Date());
      return lawRepo.save(idea);
    }
    return idea;
    //TODO: What happens with a proposal when a supporter gets removed?
  }



  private class CompareByRecentComments implements Comparator<LawModel> {
    final Map<Long, Long> numRecentComments;

    public CompareByRecentComments(Map<Long, Long> numRecentComments) {
      this.numRecentComments = numRecentComments;
    }

    @Override
    public int compare(LawModel o1, LawModel o2) {
      long count1 = numRecentComments.getOrDefault(o1.getId(), 0L);
      long count2 = numRecentComments.getOrDefault(o2.getId(), 0L);
      return Long.compare(count2, count1);   // need to reverse the order
    }
  }

  /**
   * Get recently discussed proposals. (Only proposals can be discussed!)
   * @param max maximum number of proposals to return
   * @return most discussed proposals sorted by their number of recent comments
   */
  public List<LawModel> getRecentlyDiscussed(int max) {
    Map<Long, Long> numRecentComments = new HashMap<>();
    Set<LawModel> mostDiscussedProposals = new HashSet<>();
    //Iterable<CommentModel> recentComments = commentRepo.findAll(Sort.by(Sort.Order.desc("createdAt")));   // newest comments first
    Iterable<CommentModel> recentComments = commentRepo.findAll(PageRequest.of(0, 50, Sort.by(Sort.Order.desc("createdAt"))));  // when a line of code ends with four closing brackets, then you do have to many factory methods ! :-)
    for (CommentModel c: recentComments) {
      mostDiscussedProposals.add(c.getProposal());
      Long key   = c.getProposal().getId();
      Long count = numRecentComments.getOrDefault(key, 0L);
      numRecentComments.put(key, count + 1);
    }
    List<LawModel> sortedProposals = mostDiscussedProposals.stream()
        .sorted(new CompareByRecentComments(numRecentComments))
        .limit(max)
        .collect(Collectors.toList());
    return sortedProposals;
  }

  // ============= server side search for ideas, proposals and laws ==============
  //
  // References
  // https://www.baeldung.com/rest-api-query-search-language-more-operations   Advanced REST queries
  // https://leaks.wanari.com/2018/01/23/awesome-spring-specification/         nice code


  // lovely duplicate metadata :-(     This could be created automatically by code generation. But that's overkill!
  public static final String LAW_TITLE = "title";
  public static final String LAW_DESCRIPTION = "description"; // LawModel.class.getDeclaredFields()[0].getName();
  public static final String LAW_CREATED_BY = "createdBy";
  public static final String LAW_AREA = "area";
  public static final String LAW_STATUS = "status";
  public static final String LAW_SUPPORTERS = "supporters";
  public static final String LAW_CREATED_AT = "createdAt";
  public static final String LAW_UPDATED_AT = "updatedAt";
  public static final String USER_EMAIL = "email";
  public static final String USER_NAME = "name";


  /**
   * Matches an idea, proposal or proposal by its creator
   * @param creator a user
   * @return spring data JPA {@link Specification<LawModel>}
   */
  public static Specification<LawModel> createdBy(UserModel creator) {
    return (Specification<LawModel>) (law, query, builder) -> builder.equal(law.get(LAW_CREATED_BY), creator);
  }

  public static Specification<LawModel> matchesArea(AreaModel area) {
    return (Specification<LawModel>) (law, query, builder) -> builder.equal(law.get(LAW_AREA), area);
  }

  public static Specification<LawModel> matchesStatus(LawModel.LawStatus status) {
    return (Specification<LawModel>) (law, query, builder) -> builder.equal(law.get(LAW_STATUS), status);
  }

  /** matches LawModels that have a status which is contained in the passed statusList */
  public static Specification<LawModel> matchesStatusList(List<LawModel.LawStatus> statusList) {
    return (Specification<LawModel>) (law, query, builder) -> law.get(LAW_STATUS).in(statusList);
  }

  public static Specification<LawModel> supportedBy(UserModel supporter) {
    return (Specification<LawModel>) (law, query, builder) -> builder.isMember(supporter, law.get(LAW_SUPPORTERS));   // Why does builder.isMember have the params the other way round???
  }

  public static Specification<LawModel> updatedAfter(Date after) {
    return (Specification<LawModel>) (law, query, builder) -> builder.greaterThanOrEqualTo(law.<Date>get(LAW_UPDATED_AT), after);
  }

  public static Specification<LawModel> updatedWithinDateRange(LocalDateTime from, LocalDateTime until) {
    return (Specification<LawModel>) (law, query, builder) -> builder.between(law.<LocalDateTime>get(LAW_UPDATED_AT), from, until);
  }


  public static Specification<LawModel> updatedBefore(Date before) {
    return (Specification<LawModel>) (law, query, builder) -> builder.lessThanOrEqualTo(law.<Date>get(LAW_UPDATED_AT), before);
  }

  /* This would be the "old" way of getting the necessary dependencies by hand:

  @Autowire EntityManager em;
  CriteriaBuilder cb = em.getCriteriaBuilder();
  CriteriaQuery<Message> cq = cb.createQuery(LawModel.class);
  Root<Message> root = cq.from(LawModel.class);

  These are now provided by Spring when using Specification.
  */

  /**
   * This free text search tries to match as much as possible. This specification matches if searchText is contained in
   * proposal's title or description or email of creator or name of creator. It also matches case insensitive.
   * @param searchText any string that may be contained in one of the fields.
   * @return a specification that matches LawModels or null if searchText is null.
   */
  public static Specification<LawModel> freeTextSearch(String searchText) {
    return (Specification<LawModel>) (law, query, cb) -> {
      if (searchText == null || searchText.length() == 0) return null;
      String pattern = "%"+searchText.toLowerCase()+"%";
      Join<LawModel, UserModel> creatorJoin = law.join(LAW_CREATED_BY, JoinType.LEFT);

      return cb.or(
        cb.like(cb.lower(law.get(LAW_TITLE)), pattern),
        cb.like(cb.lower(law.get(LAW_DESCRIPTION)), pattern),
        cb.like(cb.lower(creatorJoin.get(USER_EMAIL)), pattern),
        cb.like(cb.lower(creatorJoin.get("profile").get("name")), pattern)
      );
    };
  }

  /** This Specification returns a Predicate that never matches anything. */
  public static Specification<LawModel> matchesNothing() {
    return (law, query, cb) -> cb.equal(law.get(LAW_STATUS),"DOES_NEVER_MATCH_YXCVSDF");
  }

  /**
   * Match a LawModel against a set of query parameters. All query parameters are optional.
   * If more then one is given then the search parameters are AND'ed.
   * If one parameters does not match at all (e.g. the area title is not found at all), then
   * a specification that matches nothing is returned.
   *
   * @param lawQuery the query paramertes for the search
   * @return a Specification that matches this set of query parameters.
   */
  public Specification<LawModel> matchesQuery(LawQuery lawQuery) {
    List<Specification<LawModel>> specs = new ArrayList<>();

    // created by
    if (lawQuery.getCreatedByEmail().isPresent()) {
      Optional<UserModel> createdBy = userRepo.findByEmail(lawQuery.getCreatedByEmail().get().toLowerCase());
      if (!createdBy.isPresent()) return matchesNothing();    // if creator is not found return empty match.
      specs.add(createdBy(createdBy.get()));
    }

    // supported by
    if (lawQuery.getSupportedByEMail().isPresent()) {
      Optional<UserModel> supportedBy = userRepo.findByEmail(lawQuery.getSupportedByEMail().get().toLowerCase());
      if (!supportedBy.isPresent()) return matchesNothing();
      specs.add(supportedBy(supportedBy.get()));
    }

    // status
    lawQuery.getStatusList().ifPresent(statusList ->
      specs.add(matchesStatusList(statusList))
    );

    // free text search
    lawQuery.getSearchText().ifPresent(searchText ->
      specs.add(freeTextSearch(searchText))
    );

    // area title
    if (lawQuery.getAreaTitle().isPresent()) {
      Optional<AreaModel> area = areaRepo.findByTitle(lawQuery.getAreaTitle().get());
      if (!area.isPresent()) return matchesNothing();
      specs.add(matchesArea(area.get()));
    }

    // area Id
    if (lawQuery.getAreaId().isPresent()) {
      Optional<AreaModel> area = areaRepo.findById(lawQuery.getAreaId().get());
      if (!area.isPresent()) return matchesNothing();
      specs.add(matchesArea(area.get()));
    }

    // updated after
    lawQuery.getUpdatedAfter().ifPresent((after) ->
      specs.add(updatedAfter(after)));

    // updated before
    lawQuery.getUpdatedBefore().ifPresent((before) ->
      specs.add(updatedBefore(before)));


    if (specs.size() == 0) return null;   // empty query => match everything

    Specification<LawModel> spec = specs.get(0);    // there is no way to build an empty specification that always matches
    for (int i = 1; i < specs.size(); i++) {
      spec = spec.and(specs.get(i));
    }
    return spec;
  }

  /**
   * Sever side search for ideas, proposals and laws. With advanced filter capabilities.
   * @param lawQuery search criteria for LawModels
   * @return list of LawModels that match the given query
   */
  public Page<LawModel> findBySearchQuery(LawQuery lawQuery) {
    Specification<LawModel> spec = matchesQuery(lawQuery);

    /*
    Pageable pageable = lawQuery.getSortByProperties().size() == 0
      ? PageRequest.of(lawQuery.getPage(), lawQuery.getLimit())        // PageRequest.of does not accept empty properties array :-(  This call sets Sort.UNSORTED
      : PageRequest.of(lawQuery.getPage(), lawQuery.getLimit(), lawQuery.getDirection(), lawQuery.getSortByPropertiesAsStringArray());
    */

    Pageable pageable = lawQuery.getSortByProperties().size() == 0
      ? new OffsetLimitPageable(lawQuery.getOffset(), lawQuery.getLimit())
      : new OffsetLimitPageable(lawQuery.getOffset(), lawQuery.getLimit(), Sort.by(lawQuery.getDirection(), lawQuery.getSortByPropertiesAsStringArray()));

    //Implementation note: This couldn't be implemented as a LawRepoCustomImpl, because I could not autowire LawRepo in the custom impl class.
    return lawRepo.findAll(spec, pageable);
  }
}

