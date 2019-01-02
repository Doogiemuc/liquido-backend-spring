package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for voting
 */
@Slf4j
// This is a spring-data-jpa controller with all its HATEOAS magic.
// For example here you can inject the PersistentEntityResourceAssembler.
// But these methods CANNOT return a plain string as HTTP response. You MUST at least return JSON.
@BasePathAwareController
//@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)      //not necessary?
//@PreAuthorize("isAuthenticated()")    => possible but not necessary. Auth is already checked in JwtAuthenticationFilter
public class VoteRestController {

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	ProxyService proxyService;

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;


	@Autowired
	EntityLinks entityLinks;

	/**
	 * User requests a token that allows him to vote. This request MUST be authenticated!
	 * We also return delegationCount, because delegationCount can only be fetched when the token is known.
	 *
	 * @param area Area for the token
	 * @param becomePublicProxy (optional) boolean if user immediately wants to become a public proxy. Can also do so later.
	 *                          User may already be a public proxy. Even when you pass "false" he then will stay a public proxy.
	 * @return JSON with voterToken and delegationCount
	 * @throws LiquidoException when not logged in
	 */
	@RequestMapping(value = "/my/voterToken/{areaId}")  // when you add produces = MediaType.APPLICATION_JSON_VALUE  then client MUST send accept header. Without it Json is returned by default
	public @ResponseBody Lson getVoterToken(
			@PathVariable("areaId") AreaModel area,
			@RequestParam("tokenSecret") String tokenSecret,
			@RequestParam(name = "becomePublicProxy", defaultValue = "false", required = false) Boolean becomePublicProxy
			//  Authentication auth
	) throws LiquidoException {
		UserModel voter = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(()-> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to get voterToken!"));			// [SECURITY]  This check is extremely important! Only valid users are allowed to get a voterToken
		//UserModel voter = ((LiquidoAuthUser) auth.getPrincipal()).getLiquidoUserModel();   // This also works. But I kinda like getCurrentAuditor(), because it support mock auditor so nicely
		log.trace("Request voterToken for " + voter.toStringShort() + " in " + area);
		String voterToken = castVoteService.createVoterTokenAndStoreChecksum(voter, area, tokenSecret, becomePublicProxy);   // preconditions are checked inside castVoteService
		long delegationCount = proxyService.getRealDelegationCount(voterToken);

		Link areaLink = entityLinks.linkToSingleResource(AreaModel.class, area.getId());      // Spring HATEOAS Link rel
		return Lson.builder()
				.put("_links.area.href", areaLink.getHref())   		// return link to Area. Area Link has suffix {?projection} !
				.put("_links.area.templated", areaLink.isTemplated())	// is true for areas
				.put("voterToken", voterToken)
				.put("delegationCount", delegationCount);					// overall number of delegations (incl. transitive ones)
	}


	/**
	 * Post a ballot, called when a User casts his vote.
	 * Each ballot is signed with a voter's token. When a user is proxy for some other users, then this method
	 * will builder one ballot for each ballotToken that has been assigned to this proxy.
	 *
	 * This endpoint must be used instead of the default HATEOAS POST endpoint for /ballots!
	 * http://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers
	 *
	 * Example JSON payload:
	 * <pre>
	 *  {
	 *    "poll": "/liquido/v2/polls/4711",
	 *    "voterTokens": [
	 *      "asdfv532sasdfsf...",    // users own voterToken
	 *      "aasf341sdvvdaaa...",    // one token per delegee if user is a proxy
	 *      [...]
	 *    ],
	 *    "voteOrder": [
	 *      "/liquido/v2/laws/42",
	 *      "/liquido/v2/laws/43"
	 *    ]
	 *  }
	 * </pre>
	 *
	 * @param castVoteRequest the posted ballot as a REST resource
	 * @return on success JSON:
	 *   {
	 *     "msg": "OK, your ballot was counted.",
	 *     "delegees": "0",
	 *     "checksumModel": "$2a$10$1IdrGrRAN2Wp3U7QI.JIzueBtPrEreWk1ktFJ3l61Tyv4TC6ICLp2",
	 *     "poll": "/polls/253"
	 *   }
	 */
  @RequestMapping(value = "/castVote", method = RequestMethod.POST)   // @RequestMapping(value = "somePath") here on type/method level does not work with @RepositoryRestController. But it seems to work with BasePathAwareController
  @ResponseStatus(HttpStatus.CREATED)
  public @ResponseBody Map castVote(@RequestBody CastVoteRequest castVoteRequest) throws LiquidoException {
    log.trace("=> POST /castVote");
    Optional<UserModel> currentUser = liquidoAuditorAware.getCurrentAuditor();
    if (currentUser.isPresent()) throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast Vote. You should cast your vote anonymously. Do not send a SESSIONID.");

    BallotModel ballot = castVoteService.castVote(castVoteRequest);   					// all validity checks are done inside ballotService.

		HashMap<String, String> result = new HashMap<>();
		result.put("msg", "OK, your vote was counted successfully.");
		result.put("poll", castVoteRequest.getPoll());                  // return URI of poll
		result.put("checksum", ballot.getChecksum().getChecksum());     // Ballots are NOT exposed as RepositoryRestResource, therefore we return just the checksum.
		result.put("voteCount", ballot.getVoteCount()+"");
    return result;
   }






  /*
  This is how you can return spring HATEOAS JSON as response

  public @ResponseBody PersistentEntityResource someMethod(
			@RequestBody PayloadModel payload
			PersistentEntityResourceAssembler resourceAssembler
	) {

		// cannot return simple string in Spring :-( http://stackoverflow.com/questions/30895286/spring-mvc-how-to-return-simple-string-as-json-in-rest-controller/30895501
		return resourceAssembler.toResource(responseJpaEntity)
	}

	 * Remark: POST for builder, PUT would be for update! See http://stackoverflow.com/questions/630453/put-vs-post-in-rest?rq=1
		 *
		 * Related resources for @RepositoryRestController
		 * Example by Oliver Ghierke:  https://github.com/olivergierke/spring-restbucks
		 * https://jira.spring.io/browse/DATAREST-972   - creatd by ME   WOW discussion ongoing :-)
		 * https://jira.spring.io/browse/DATAREST-633
		 * https://jira.spring.io/browse/DATAREST-687
		 * http://stackoverflow.com/questions/40986738/spring-data-rest-no-string-argument-constructor-factory-method-to-deserialize/40986739   fixes "no String-argument constructor/factory method to deserialize from String value"
		 * http://stackoverflow.com/questions/38607421/spring-data-rest-controllers-behaviour-and-usage-of-basepathawarecontroller   +++
		 * http://stackoverflow.com/questions/31758862/enable-hal-serialization-in-spring-boot-for-custom-controller-method?rq=1    with reply from Oliver Gierke himself :-)
		 * http://stackoverflow.com/questions/31924980/filling-entity-links-in-custom-repositoryrestcontroller-methods
		 * http://stackoverflow.com/questions/39612434/repositoryrestcontroller-makes-rest-api-not-available
		 *
		 * Posting to One-To-Many subresource via REST (in HATEOAS) this is what I am overwriting here.
		 * https://stackoverflow.com/questions/25311978/posting-a-onetomany-sub-resource-association-in-spring-data-rest
		 * https://stackoverflow.com/questions/37902946/add-item-to-the-collection-with-foreign-key-via-rest-call
		 *
		 * ERROR: "no String-argument constructor/factory method to deserialize from String value"
		 * Solution: http://stackoverflow.com/questions/40986738/spring-data-rest-no-string-argument-constructor-factory-method-to-deserialize/40986739
		 *           https://jira.spring.io/browse/DATAREST-884   =>


			//@AuthenticationPrincipal(expression = "liquidoUserModel") UserModel liquidoUserModel    // <==== DOES NOT WORK
			// injecting the AuthenticationPrincipal did not work for me. I do not know why.   But liquidoAuditorAware works, and is also great for testing:
			// see https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#tech-userdetailsservice


   */

}



