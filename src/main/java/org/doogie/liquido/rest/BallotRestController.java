package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CastVoteDTO;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.BallotService;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
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
public class BallotRestController {

	@Autowired
	BallotService ballotService;

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;


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
   * @param castVoteResource the posted ballot as a REST resource
   * @return an default ok message (JSON)
   */
  @RequestMapping(value = "/postBallot", method = RequestMethod.POST)   // @RequestMapping(value = "somePath") here on type/method level does not work with @RepositoryRestController. But it seems to work with BasePathAwareController
  @ResponseStatus(HttpStatus.CREATED)
  public @ResponseBody /*PersistentEntityResource*/ Map postBallot(
      @RequestBody Resource<CastVoteDTO> castVoteResource
      //@AuthenticationPrincipal UserModel principalUserModel,
      //PersistentEntityResourceAssembler resourceAssembler
  ) throws LiquidoException {
    // Implementation note: Must use a Resource<CastVoteDTO> as RequestBody instead of just a CastVoteDTO, because only this way the deserialization
    // from URI to models is automatically handled by spring-data-rest.
    log.trace("=> POST /postBallot");

	  //log.trace(principalUserModel.toString());

    UserModel currentUser = liquidoAuditorAware.getCurrentAuditor();
    if (currentUser == null) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "Cannot postBallot. Need an authenticated user as fromUser");

    // Check validity of posted ballot JSON
    CastVoteDTO castVoteDTO = castVoteResource.getContent();
	  BallotModel savedBallot = ballotService.castVote(currentUser, castVoteDTO);

	  // Keep in mind that the savedBallot object is not completely filled, e.g. you cannot call toString on it.    => REALLY? :-)
    log.trace("<= POST /postBallot: pollId="+savedBallot.getPoll().getId());
    //return resourceAssembler.toResource(savedBallot);

    // cannot return simple string in Spring :-( http://stackoverflow.com/questions/30895286/spring-mvc-how-to-return-simple-string-as-json-in-rest-controller/30895501
    Map<String, String> result = new HashMap<>();
    result.put("msg", "OK, your ballot was counted.");
    result.put("poll.id", savedBallot.getPoll().getId().toString());
    result.put("areaToken", savedBallot.getAreaToken());        // with this area token, the voter can later confirm that his vote was counted for.
    result.put("delegees", currentUser.getVoterTokens().size()+"");
    return result;
  }

}



