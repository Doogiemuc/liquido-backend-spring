package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for posting a ballot
 *
 * Resouces:
 *   POST /postBallot  -  post a users vote
 *
 */
@Slf4j
@BasePathAwareController
//@RepositoryRestController   and    @RequestMapping("postBallot")    Both do not really work
//@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)      //not necessary?
//TODO: @PreAuthorize("isAuthenticated()")    or is my security configuration already enough?
public class VoteRestController {

	@Autowired
	CastVoteService ballotService;

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;

	/**
	 * User requests a token that allows him to vote.
	 * This request MUST be authenticated!
	 * @param area Area for the token
	 * @return JSON with voterToken
	 * @throws LiquidoException when request parameter is missing
	 */
	@RequestMapping(value = "/voterToken", method = RequestMethod.GET)
	public @ResponseBody Map getVoterToken(@RequestParam("area")AreaModel area /*@AuthenticationPrincipal User authUser, Principal principal*/) throws LiquidoException {
		// injecting the AuthenticationPrincipal did not work for me. I do not know why.   But liquidoAuditorAware works, and is also great for testing:
		UserModel user = liquidoAuditorAware.getCurrentAuditor();
		log.info(user+" requests a voter token for area "+area);
		String voterToken = ballotService.getVoterToken(user, area);   // preconditions are checked inside ballotService
		Map<String, String> result = new HashMap<>();
		result.put("voterToken", voterToken);
		return result;
	}



		/**
		 * Post a ballot, called when a User casts his vote.
		 * Each ballot is signed with a voter's token. When a user is proxy for some other users, then this method
		 * will create one ballot for each ballotToken that has been assigned to this proxy.
		 *
		 * This endpoint must be used instead of the default HATEOAS POST endpoint for /ballots!
		 * http://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers
		 *
		 * Example JSON payload:
		 * <pre>
		 *  {
		 *    "poll": "/liquido/v2/polls/4711",
		 *    "voterToken": "asdfv532sasdfsf...",
		 *    "voteOrder": [
		 *      "/liquido/v2/laws/42",
		 *      "/liquido/v2/laws/43"
		 *    ]
		 *  }
		 * </pre>
		 *
		 * Remark: POST for create, PUT would be for update! See http://stackoverflow.com/questions/630453/put-vs-post-in-rest?rq=1
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
		 *
		 * @param castVoteRequest the posted ballot as a REST resource
		 * @return on success JSON:
		 *   {
		 *     "msg": "OK, your ballot was counted.",
		 *     "delegees": "0",
		 *     "checksum": "$2a$10$1IdrGrRAN2Wp3U7QI.JIzueBtPrEreWk1ktFJ3l61Tyv4TC6ICLp2",
		 *     "poll": "/polls/253"
		 *   }
		 */
		//TODO:  map this under /polls/<id>/castVote
  @RequestMapping(value = "/castVote", method = RequestMethod.POST)   // @RequestMapping(value = "somePath") here on type/method level does not work with @RepositoryRestController. But it seems to work with BasePathAwareController
  @ResponseStatus(HttpStatus.CREATED)
  public @ResponseBody /*PersistentEntityResource*/ Map castVote(
      @RequestBody CastVoteRequest castVoteRequest
      //@AuthenticationPrincipal UserModel principalUserModel,
      //PersistentEntityResourceAssembler resourceAssembler
  ) throws LiquidoException {
    log.trace("=> POST /castVote");
    UserModel currentUser = liquidoAuditorAware.getCurrentAuditor();
    if (currentUser == null) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "Cannot postBallot. Need an authenticated user as fromUser");

    BallotModel savedBallot = ballotService.castVote(currentUser, castVoteRequest);   // all validity checsk are done inside ballotService.

	  // Keep in mind that the savedBallot object is not completely filled, e.g. you cannot call toString on it.    => REALLY? :-)
    log.trace("<= POST /castVote: pollId="+savedBallot.getPoll().getId());
    //return resourceAssembler.toResource(savedBallot);

    // cannot return simple string in Spring :-( http://stackoverflow.com/questions/30895286/spring-mvc-how-to-return-simple-string-as-json-in-rest-controller/30895501
    Map<String, String> result = new HashMap<>();
    result.put("msg", "OK, your ballot was counted.");
    result.put("poll", "/polls/"+savedBallot.getPoll().getId());
    result.put("checksum", savedBallot.getChecksum());        // with this checksum, the voter can later confirm that his vote in this poll was counted for.
    result.put("delegees", currentUser.getVoterTokens().size()+"");
    // We do not send the voteOrder back in the response, because the voterOrder must not be related to this user's IP.
    return result;
  }

}



