package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller for our RESTfull endpoint: /ballot
 *
 * Resouces:
 *   POST /postBallot  -  post a users vote
 *
 */
@BasePathAwareController
//@RepositoryRestController   nad    @RequestMapping("postBallot")   does not really work
//@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)      //not necessary?
public class BallotRestController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  BallotRepo ballotRepo;

  //TODO: need to move to its own controller
  /**
   * Simple is alive test
   * @return <pre>{"Hello":"World"}</pre>

  @RequestMapping("/ping")
  public String greeting() {
    log.trace("=> GET /ping");
    return "{\"Hello\":\"World\"}";
  }
  */


  /**
   * Post a ballot, ie. a vote from a user
   *
   * This customizes the default HATEOAS endpoint for /ballots!
   * http://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers
   *
   * Remark: POST for create, PUT would be for update! See http://stackoverflow.com/questions/630453/put-vs-post-in-rest?rq=1
   *
   * Related resources for @RepositoryRestController
   * Example by Oliver Ghierke:  https://github.com/olivergierke/spring-restbucks
   * https://jira.spring.io/browse/DATAREST-972   - creatd by ME
   * https://jira.spring.io/browse/DATAREST-633
   * http://stackoverflow.com/questions/38607421/spring-data-rest-controllers-behaviour-and-usage-of-basepathawarecontroller   +++
   * http://stackoverflow.com/questions/31758862/enable-hal-serialization-in-spring-boot-for-custom-controller-method?rq=1    with reply from Oliver Gierke himself :-)
   * http://stackoverflow.com/questions/31924980/filling-entity-links-in-custom-repositoryrestcontroller-methods
   * http://stackoverflow.com/questions/39612434/repositoryrestcontroller-makes-rest-api-not-available
   *
   * ERROR: "no String-argument constructor/factory method to deserialize from String value"
   * Solution: http://stackoverflow.com/questions/40986738/spring-data-rest-no-string-argument-constructor-factory-method-to-deserialize/40986739
   *
   * @param newBallotRes the posted ballot as a REST resource
   * @param resourceAssembler injected PersistentEntityResourceAssembler that can build the reply
   * @return the stored ballot (incl. its new ID) as a HATEOAS resource
   */
  @RequestMapping(value = "/postBallot", method = POST)   // @RequestMapping(value = "somePath") here on type/method level does not work with @RepositoryRestController
  @ResponseStatus(HttpStatus.CREATED)
  public @ResponseBody PersistentEntityResource postBallot(@RequestBody Resource<BallotModel> newBallotRes,
                            PersistentEntityResourceAssembler resourceAssembler)
  {
    log.trace("=> POST /postBallot "+newBallotRes);
    BallotModel newBallot = newBallotRes.getContent();

    // check validity of voterHash
    if (newBallot.getVoterHash() == null || newBallot.getVoterHash().length() < 5) {
      throw new LiquidoRestException("ERROR: Cannot post ballot: Invalid voterHash: '"+newBallot.getVoterHash()+"'. Must be at least 5 chars!");
    }

    // check that initialProposal is actually in voting phase
    LawModel initialProposal = newBallot.getInitialProposal();
    if (initialProposal == null || !LawModel.LawStatus.VOTING.equals(initialProposal.getStatus())) {
      throw new LiquidoRestException("ERROR: Cannot post ballot: InitialLaw must be in voting phase.");
    }

    // check that voter Order is not empty
    if (newBallot.getVoteOrder() == null || newBallot.getVoteOrder().size() == 0) {
      throw new LiquidoRestException("ERROR: Cannot post ballot: VoteOrder is empty!");
    }

    BallotModel createdBallot = ballotRepo.save(newBallotRes.getContent());
    log.trace("<= POST /postBallot created: "+createdBallot);

    return resourceAssembler.toResource(createdBallot);
    // return ResponseEntity.ok(createdBallot);   this would return a ResponseEntity with the plain serialized JSON  (no HAL links etc.)
  }

  // MAYBE also look at JSON-Patch. Could that be used here to post the voteOrder?
  // http://stackoverflow.com/questions/25311978/posting-a-onetomany-sub-resource-association-in-spring-data-rest
  // https://github.com/spring-projects/spring-data-rest/commit/ef3720be11f117bb691edbbf63e38ff72e0eb3dd
  // http://stackoverflow.com/questions/34843297/modify-onetomany-entity-in-spring-data-rest-without-its-repository/34864254#34864254

}



