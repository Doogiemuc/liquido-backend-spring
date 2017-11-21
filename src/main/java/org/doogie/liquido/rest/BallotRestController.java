package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.model.*;
import org.doogie.liquido.security.LiquidoAnonymizer;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Controller for posting a ballot
 *
 * Resouces:
 *   POST /postBallot  -  post a users vote
 *
 */
@BasePathAwareController
//@RepositoryRestController   and    @RequestMapping("postBallot")    Both do not really work
//@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)      //not necessary?
public class BallotRestController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  BallotRepo ballotRepo;

  @Autowired
  DelegationRepo delegationRepo;

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
  LiquidoAnonymizer anonymizer;

  /**
   * Post a ballot, called when a User casts his vote.
   * Each ballot is signed with a voter's token. When a user is proxy for some other users, then this method
   * will create one ballot for each voterToken that has been assigned to this proxy.
   *
   * This endpoint must be used instead of the default HATEOAS POST endpoint for /ballots!
   * http://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers
   *
   * Example JSON payload:
   * <pre>
   *  {
   *    "initialProposal": "/liquido/v2/laws/42",
   *     "voteOrder": [
   *       "/liquido/v2/laws/42",
   *       "/liquido/v2/laws/43"
   *     ]
   *  }
   * </pre>
   *
   * Remark: POST for create, PUT would be for update! See http://stackoverflow.com/questions/630453/put-vs-post-in-rest?rq=1
   *
   * Related resources for @RepositoryRestController
   * Example by Oliver Ghierke:  https://github.com/olivergierke/spring-restbucks
   * https://jira.spring.io/browse/DATAREST-972   - creatd by ME
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
   * @param ballotResource the posted ballot as a REST resource
   * @param resourceAssembler injected PersistentEntityResourceAssembler that can build the reply
   * @return an default ok message (JSON)
   */
  @RequestMapping(value = "/postBallot", method = RequestMethod.POST)   // @RequestMapping(value = "somePath") here on type/method level does not work with @RepositoryRestController. But it seems to work with BasePathAwareController
  @ResponseStatus(HttpStatus.CREATED)
  public @ResponseBody /*PersistentEntityResource*/ Map postBallot(
      @RequestBody Resource<BallotModel> ballotResource,
      PersistentEntityResourceAssembler resourceAssembler)
  {
    // Implementation note: Must use a Resource<BallotModel> as RequestBody instead of just a BallotModel, because only this way the deserialization
    // from URI to models is automatically handled by spring-data.

    log.trace("=> POST /postBallot");
    UserModel currentUser = liquidoAuditorAware.getCurrentAuditor();
    if (currentUser == null) throw new LiquidoRestException("Cannot postBallot. Need an authenticated user as fromUser");

    // Check validity of posted ballot JSON
    BallotModel postedBallot = ballotResource.getContent();
    // Create a voterToken for currentUser and store a ballot for him
    String voterTokenBCrypt = anonymizer.getBCryptVoterToken(currentUser, currentUser.getPasswordHash(), postedBallot.getPoll());
    postedBallot.setVoterToken(voterTokenBCrypt);
    checkBallot(postedBallot);

    // Check that this voter (if he is a proxy) has as many voter tokens as he has delegations
    AreaModel area = postedBallot.getPoll().getProposals().iterator().next().getArea();
    long numVotes = delegationRepo.getNumVotes(area, currentUser);
    if (numVotes != currentUser.getVoterTokens().size()+1)
      throw new LiquidoRestException("ERROR: Inconsistency detected: number of voter tokens does not match number of delegations. User '"+currentUser.getEmail()+"' (id="+currentUser.getId()+")");

    // Check if user has already voted. If so, then update the voteOrder in his existing ballot
    BallotModel savedBallot = null;
    BallotModel existingBallot = ballotRepo.findByPollAndVoterToken(postedBallot.getPoll(), postedBallot.getVoterToken());
    if (existingBallot != null) {
      existingBallot.setVoteOrder(postedBallot.getVoteOrder());
      log.trace("Updating existing ballot");
      savedBallot = ballotRepo.save(existingBallot);  // update the existing ID
    } else {
      log.trace("Inserting new ballot");
      savedBallot = ballotRepo.save(postedBallot);    // insert a new BallotModel
    }

    //If this user is a proxy, then also post a ballot for each of his delegations
    for (String delegatedToken : currentUser.getVoterTokens()) {
      //TODO: check if delegee already voted for himself.  Never overwrite ballots with   ownVote == true
      BallotModel ballotForDelegee = new BallotModel(postedBallot.getPoll(), postedBallot.getVoteOrder(), delegatedToken);
      checkBallot(ballotForDelegee);
      log.trace("Saving ballot for delegee: "+ballotForDelegee);
      ballotRepo.save(ballotForDelegee);
    }

    // Keep in mind that the savedBallot object is not completely filled, e.g. you cannot call toString on it.
    log.trace("<= POST /postBallot: pollId="+savedBallot.getPoll().getId()+", voterToken="+savedBallot.getVoterToken());
    //return resourceAssembler.toResource(savedBallot);

    // cannot return simple string in Spring :-( http://stackoverflow.com/questions/30895286/spring-mvc-how-to-return-simple-string-as-json-in-rest-controller/30895501
    Map<String, String> result = new HashMap<>();
    result.put("msg", "OK, your ballot was counted.");
    result.put("voterToken", savedBallot.getVoterToken());
    result.put("delegees", currentUser.getVoterTokens().size()+"");
    return result;
  }

  /**
   * Check if the passed ballot is valid.
   * @param newBallot a casted vote
   * @throws LiquidoRestException when something inside newBallot is invalid
   */
  private void checkBallot(BallotModel newBallot) throws LiquidoRestException {
    // check that poll is actually in voting phase and has at least two alternative proposals
    PollModel poll = newBallot.getPoll();
    if (poll == null || !PollModel.PollStatus.VOTING.equals(poll.getStatus())) {
      throw new LiquidoRestException("ERROR: Cannot post ballot: Poll must be in voting phase.");
    }
    if (poll.getProposals().size() < 2)
      throw new LiquidoRestException("ERROR: Cannot post ballot: Poll must have at least two alternative proposals.");

    // check that voter Order is not empty
    if (newBallot.getVoteOrder() == null || newBallot.getVoteOrder().size() == 0) {
      throw new LiquidoRestException("ERROR: Cannot post ballot: VoteOrder is empty!");
    }

    // check that there is no duplicate vote for any one proposal
    HashSet<Long> proposalIds = new HashSet<>();
    for(LawModel proposal : newBallot.getVoteOrder()) {
      if (proposalIds.contains(proposal.getId())) {
        throw new LiquidoRestException("ERROR: Cannot post ballot: Duplicate vote for proposal_id="+proposal.getId());
      } else {
        proposalIds.add(proposal.getId());
      }
    }

    // check that all proposals you wanna vote for are also in voting phase
    for(LawModel proposal : newBallot.getVoteOrder()) {
      if (!LawModel.LawStatus.VOTING.equals(proposal.getStatus())) {
        throw new LiquidoRestException(("ERROR: Cannot post ballot: proposals must be in voting phase."));
      }
    }

    // check validity of voterToken
    if (newBallot.getVoterToken() == null || newBallot.getVoterToken().length() < 5) {
      throw new LiquidoRestException("ERROR: Cannot post ballot: Invalid voterToken: '"+newBallot.getVoterToken()+"'. Must be at least 5 chars!");
    }
  }

  // MAYBE also look at JSON-Patch. Could that be used here to post the voteOrder?
  // http://stackoverflow.com/questions/25311978/posting-a-onetomany-sub-resource-association-in-spring-data-rest
  // https://github.com/spring-projects/spring-data-rest/commit/ef3720be11f117bb691edbbf63e38ff72e0eb3dd
  // http://stackoverflow.com/questions/34843297/modify-onetomany-entity-in-spring-data-rest-without-its-repository/34864254#34864254


  /* This is how to manually create a HASH with Java's built-in SHA-256

  try {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(DoogiesUtil.longToBytes(userId));
    md.update(DoogiesUtil.longToBytes(initialPropId));
    md.update(userPassword.getBytes());
    byte[] digest = md.digest();
    String voterTokenSHA256 = DoogiesUtil.bytesToString(digest);
  } catch (NoSuchAlgorithmException e) {
    log.error("FATAL: cannot create SHA-256 MessageDigest: "+e);
    throw new LiquidoRestException("Internal error in backend");
  }
  */


}



