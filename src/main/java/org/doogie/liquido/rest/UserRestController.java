package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.model.*;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.*;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rest Controller for registration, login and user data
 */
@Slf4j
// ******************************************************************************************
// If you want to return text/plain from any controller method, then you MUST use a @RestController
// A @BasePathAwareController has all the spring-data-jpa magic, but it cannot return plain string.
// See
// https://stackoverflow.com/questions/38607421/spring-data-rest-controllers-behaviour-and-usage-of-basepathawarecontroller
// https://stackoverflow.com/questions/33687722/spring-data-rest-custom-method-return-string
// https://jira.spring.io/browse/DATAREST-1323  (by me)
// ******************************************************************************************
// I want this to be a normal Spring Rest controller.   No spring-dta-jpa magic.
// Since this a normal RestController, we do not need the @ResponseBody annotation on every method.
@RestController
@RequestMapping("${spring.data.rest.base-path}")
public class UserRestController {
	private static final int MIN_PASSWORD_LENGTH = 8;

	@Autowired
	Environment springEnv;

	@Autowired
	LiquidoProperties prop;

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
	UserRepo userRepo;

  @Autowired
	AreaRepo areaRepo;

  @Autowired
	LawRepo lawRepo;

  @Autowired
	PollRepo pollRepo;

  @Autowired
	DelegationRepo delegationRepo;

  @Autowired
	ProxyService proxyService;

  @Autowired
  OneTimeTokenRepo ottRepo;

	@Autowired
	ProjectionFactory factory;

  @Autowired
	JwtTokenUtils jwtTokenUtils;

  @Autowired
	MailService mailService;

  @Autowired
	CastVoteService castVoteService;

  @Autowired
	BallotRepo ballotRepo;

  @Autowired
	UserService userService;

  /**
	 * Register as a new user. This will also create the authy user at twilio.com
	 * @param newUser UserModel with at least email and profile.mobilePhone.
	 * @return HTTP OK(200)   or error 400 when data is missing or user already exists
	 * @throws LiquidoException when newUser is not ok.
	 */
	@RequestMapping(path = "/auth/register", method = RequestMethod.POST)
	public ResponseEntity registerNewUser(@RequestBody UserModel newUser) throws LiquidoException {
		log.info("Register new user "+newUser);
		UserModel savedUser = userService.registerUser(newUser);
		return ResponseEntity.ok(savedUser);   // savedUser now has an authyId
	}

	/**
	 * Request a one time token for login. This will check if a user with that mobile exists.
	 * The request will be forward to Twilio for authentication.
	 * The one time token entered by the user can then be verified with a call to <pre>/auth/loginWithToken</pre>
	 * @param mobile users mobile phone number. MUST be a known user. Mobile number will be cleaned.
	 * @return HttpStatus.OK, when code was sent via SMS
	 *        OR HttpStatus.NOT_FOUND when cleaned mobile phone number was not found and user must register first.
	 */
  @RequestMapping(value = "/auth/requestToken", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity requestSmsToken(@RequestParam("mobile") String mobile) throws LiquidoException {
		log.info("Push authentication request for mobile="+mobile);
		userService.requestPushAuthentication(mobile);
		return ResponseEntity.ok().build();
  }

	/**
	 * login with a one time token
	 * This endpoint must be public.
	 * @param token the 6 digit code from the SMS
	 * @return Json Web Token (JWT) with user data encoded within it.
	 */
  @RequestMapping(path = "/auth/loginWithToken", produces = MediaType.TEXT_PLAIN_VALUE)
  public String loginWithSmsToken(
			@NonNull @RequestParam("mobile") String mobile,
			@NonNull @RequestParam("token") String token
  ) throws LiquidoException {
	  log.info("Request to login with token="+token);
		UserModel user = userService.verifyOneTimePassword(mobile, token);
		// TODO: when user is member or admin in more than one team
		return jwtTokenUtils.generateToken(user.getId(), null);
  }


	/**
	 * Request a login token via email. When passed email is valid, ie. user is registered and known
	 * then an email with a login link will be sent to his email account.
	 * @param email must be a valid, previously registered email adress
	 * @return HTTP OK
	 * @throws LiquidoException when this email is not yet registered (or mail cannot be sent via SMTP)
	 */
	@RequestMapping(value = "/auth/requestEmailToken")
	public ResponseEntity requestEmailToken(@RequestParam("email") String email) throws LiquidoException {
		if (DoogiesUtil.isEmpty(email)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND,  "Need email!");
		log.info("request email login code for email="+email);
		UserModel user = userRepo.findByEmail(email)
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND,  "No user found with email "+email+". You must register first."));

		// Create new email login link with a token time token in it.
		UUID emailToken = UUID.randomUUID();
		LocalDateTime validUntil = LocalDateTime.now().plusHours(1);  // login token via SMS is valid for one hour. That should be enough!
		OneTimeToken oneTimeToken = new OneTimeToken(emailToken.toString(), user, validUntil);
		ottRepo.save(oneTimeToken);
		log.info("User "+user.getEmail()+" may login. Sending code via EMail.");

		// This link is parsed in a cypress test case. Must update test if you change this.
		String loginLink = "<a id='loginLink' style='font-size: 20pt;' href='"+prop.frontendUrl+"/login?email="+user.getEmail()+"&emailToken="+oneTimeToken.getNonce()+"'>Login " + user.getName() +  "</a>";
		String body = String.join(
			System.getProperty("line.separator"),
			"<html><h1>Liquido Login Token</h1>",
			"<h3>Hello " + user.getName() + "</h3>",
			"<p>With this link you can login to Liquido.</p>",
			"<p>&nbsp;</p>",
			"<b>" + loginLink + "</b>",
			"<p>&nbsp;</p>",
			"<p>This login link can only be used once!</p>",
			"<p style='color:grey; font-size:10pt;'>You received this email, because  a login token for the <a href='https://www.liquido.net'>LIQUIDO</a> eVoting webapp was requested. If you did not request a login yourself, than you may simply ignore this message.</p>",
			"</html>"
		);
		try {
			mailService.sendEMail(user.getName(), email, body);
		} catch (Exception e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_INTERNAL_ERROR, "Internal server error: Cannot send Email "+e.toString(), e);
		}

		if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
			return ResponseEntity.ok().header("token", emailToken.toString()).build();  // when debugging, return the code in the header
		} else {
			return ResponseEntity.ok().build();
		}
	}

	/**
	 * Login via link in email. The link contains a one time token.
	 * The user and which team is stored together with the OTT.
	 * @param email user's email that MUST match the email stored together with the token
	 * @param token must be a valid login token
	 * @return Json Web Token as plain text
	 * @throws LiquidoException when token is invalid or expired
	 */
	@RequestMapping(path = "/auth/loginWithEmailToken", produces = MediaType.TEXT_PLAIN_VALUE)
	//TODO: @Transactional  I think loginWithEmailToken should be transactional. => TEST!
	public String loginWithEmailToken(
		@RequestParam("email") String email,
		@RequestParam("token") String token
	) throws LiquidoException {
		log.debug("Request to login with email token="+token);
		if (DoogiesUtil.isEmpty(email)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND, "Need email to login!");
		if (DoogiesUtil.isEmpty(token)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Need login token!");

		OneTimeToken oneTimeToken = ottRepo.findByNonce(token)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Invalid email login token."));
		if (!email.equals(oneTimeToken.getUser().getEmail()))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Email does not match token.");
		if (LocalDateTime.now().isAfter(oneTimeToken.getValidUntil())) {
			ottRepo.delete(oneTimeToken);
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This email login token is expired.");
		}

		//TODO: Do I need to check if user and team still exist?
		UserModel user = oneTimeToken.getUser();

		// return JWT token for this user
		String jwt = jwtTokenUtils.generateToken(user.getId(), null);
		oneTimeToken.getUser().setLastLogin(LocalDateTime.now());
		userRepo.save(user);
		ottRepo.delete(oneTimeToken);				//---- delete used one time token !
		log.info(user+ "logged in with valid email code.");
		return jwt;
	}

	@RequestMapping(path = "/my/newsfeed", produces = MediaType.APPLICATION_JSON_VALUE)
	public Lson getMyNewsfeed(@RequestParam("voterToken") Optional<String> voterToken) throws LiquidoException {
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Must be logged in to get newsfeed!"));

		LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);

		// users ideas that recently reached their quorum and became a proposal
		List<LawModel> reachedQuorum = lawRepo.findByReachedQuorumAtGreaterThanEqualAndCreatedBy(twoWeeksAgo, currentUser);

		// own proposals that are in a poll which is in voting phase
		List<LawModel> ownProposalsInVoting = lawRepo.findDistinctByStatusAndCreatedBy(LawModel.LawStatus.VOTING, currentUser);
		List<PollModel> pollsInVotingWithOwnProposals = ownProposalsInVoting.stream().map(p -> p.getPoll()).collect(Collectors.toList());

		//List<LawProjection> ownPropsInVotingProjected = ownProposalsInVoting.stream().map(p -> factory.createProjection(LawProjection.class, p)).collect(Collectors.toList());

		// ideas and proposals that are supported by current user
		List<LawModel> supportedByYou = new ArrayList<>();
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.IDEA, currentUser));
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.PROPOSAL, currentUser));
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.ELABORATION, currentUser));
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.VOTING, currentUser));
		List<LawModel> supportedByYouSorted = supportedByYou.stream()
				.limit(10)
				.sorted((p1, p2) -> (int) (p1.getUpdatedAt().getTime() - p2.getUpdatedAt().getTime()))
				.collect(Collectors.toList());


		// Own proposals that have recent comments
		List<LawModel> recentlyDiscussed = lawRepo.getRecentlyDiscussed(java.sql.Timestamp.valueOf(twoWeeksAgo), currentUser);

		// Delegation requests in all areas
		Iterable<AreaModel> areas = areaRepo.findAll();
		List<DelegationModel> delegationRequests = new ArrayList<>();
		for (AreaModel area: areas) {
			delegationRequests.addAll(delegationRepo.findDelegationRequests(area, currentUser));
		}

		Lson result = new Lson()
				.put("delegationRequests", delegationRequests)
				.put("reachedQuorum", reachedQuorum)
				.put("supportedByYou", supportedByYouSorted)							// This only returns the Model. No HATEOAS links! But entities are projected, with createdBy and are info expanded.
				.put("pollsInVotingWithOwnProposals", pollsInVotingWithOwnProposals)
				.put("recentlyDiscussedProposals", recentlyDiscussed);

	  //stuff by others
		//result.put("trendingProposals", trendingProposals)    //TODO: what are "trendingProposals"=> improve lawService.getRecentlyDiscussed

		return result;



	}


	// There is no logout here. The server is stateless! When a user want's to "log out", then he simply has to throw away his JWT
}




