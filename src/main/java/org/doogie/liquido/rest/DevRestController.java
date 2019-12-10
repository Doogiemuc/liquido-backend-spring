package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.data.LiquidoProperties;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * This controller is only available for development and testing
 */
@Slf4j
@RestController
@RequestMapping("${spring.data.rest.base-path}")    //TODO: add /dev prefix here!
//@Profile({"dev", "test"})			// For security reasons this controller should not be available in PROD. But I need it when I want to run tests against prod!
public class DevRestController {

	@Autowired
	UserRepo userRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	PollRepo pollRepo;

	@Autowired
	BallotRepo ballotRepo;

	@Autowired
	PollService pollService;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	LiquidoProperties prop;

	/**
	 * Get list of users for the quick login at the top right of the UI.
	 * This endpoint must be public, because the web app needs it during very early application start. (See main.js) But client must at least provide devLoginSmsToken.
	 * This is only available in dev and test environment!
	 * @param token dev login sms token
	 * @return UserModel HATEOAS resource
	 */
	@Profile({"dev", "test"})
	@RequestMapping(value = "/dev/users")
	// This must be public, because during DEV we need the list of users for the quick DevLogin drop down.
	public @ResponseBody Lson devGetAllUsers(@RequestParam("token") String token) throws LiquidoException {
		log.debug("GET /dev/users");
		if (!token.equals(prop.devLoginSmsToken))
			throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Invalid token for GET /dev/users");
		Iterator<UserModel> it = userRepo.findAll().iterator();
		List<UserModel> first10Users = new ArrayList<UserModel>();
		for (int i = 0; i < 20; i++) {
			if (it.hasNext()) first10Users.add(it.next());
		}

		ControllerLinkBuilder controllerLinkBuilder = linkTo(methodOn(DevRestController.class).devGetAllUsers(null));
		return Lson.builder()
			.put("_embedded.users", first10Users)  // Always return at least an empty array! Be nice to your clients!
			.put("_link.self.href", controllerLinkBuilder.toUri());

		//return new Resources<>(userRepo.findAll(), linkTo(methodOn(DevRestController.class).devGetAllUsers()).withRel("self"));
	}

	/**
	 * Receive a Json Web Token for authentication. This for example allows tests to login as <b>any</b> user.
	 * But the client must still provide the valid devLoginSmsToken from application.properties
	 * Then authentication for further requests is handled normally in {@link org.doogie.liquido.security.LiquidoUserDetailsService#loadUserByUsername(String)}
	 *
	 * @param mobile mobilephone of existing user
	 * @param token  devLoginSmsToken from application.properties
	 * @return Json Web Token for this user
	 * @throws LiquidoException when token is invalid or no user with that mobile is phone
	 */
	@RequestMapping(path = "/dev/getJWT", produces = MediaType.TEXT_PLAIN_VALUE)
	public String loginWithSmsToken(
		@RequestParam("mobile") String mobile,
		@RequestParam("token") String token
	) throws LiquidoException {
		if (!token.equals(prop.devLoginSmsToken))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Dev Login failed. Token for mobile="+mobile+" is invalid!");
		UserModel user = userRepo.findByProfileMobilephone(mobile)
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "DevLogin: user for mobile phone " + mobile + " not found."));
		log.info("DEV Login: " + user);
		user.setLastLogin(LocalDateTime.now());
		userRepo.save(user);
		return jwtTokenProvider.generateToken(user.getEmail());

	}

	/**
	 * Spring Web Security: Make /dev/users endpoint publicly available.  But just this one resource! The other /dev/**
	 * endpoints need authentication in ROLE_ADMIN
   */
	@Configuration
	@Order(20)   // MUST be smaller than 100  to be first!
  public class DevEndpointSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
		@Value("${spring.data.rest.base-path}")
		String basePath;

		@Autowired
		Environment springEnv;

		/**
		 * Allow anonymous access to <pre>/dev/users</pre> in DEV or TEST environment
     */
		protected void configure(HttpSecurity http) throws Exception {
			log.info("Configuring WebSecurity for Development api endpoint " + basePath + "/dev in env=" + Arrays.toString(springEnv.getActiveProfiles()));
			//nice stackoverflow question about this nasty confusing fluid syntax of HttpSecurity:  https://stackoverflow.com/questions/28907030/spring-security-authorize-request-for-url-method-using-httpsecurity
			http.antMatcher(basePath + "/dev/*").authorizeRequests()
				.antMatchers(basePath + "/dev/users").permitAll()
				.antMatchers(basePath + "/dev/getJWT").permitAll()
				.anyRequest().authenticated();
		}
	}


	/**
	 * Manually start the voting phase of a poll via REST call. Poll MUTS be in ELABORATION phase.
	 * This is used in tests, because wraping time is so complicated.
	 * @param poll a poll that must be in ELABORATION phase
	 * @return HTTP 200 and Ok message as JSON
	 * @throws LiquidoException for example when voting phase cannot be started because of wrong status in poll
	 */
	@RequestMapping(value = "/dev/polls/{pollId}/startVotingPhase")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public @ResponseBody Lson devStartVotingPhase(@PathVariable(name="pollId") PollModel poll) throws LiquidoException {
		if (poll == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Cannot find poll with that id");
		log.info("DEV: Starting voting phase of "+poll);
		pollService.startVotingPhase(poll);
		return new Lson().put("ok", "Started voting phase of poll.id="+poll.id);
	}

	/**
	 * Manually finish the voting phase of a poll. This is used in tests.
	 * @param poll a poll that must be in VOTING phase
	 * @return HTTP 200 and a JSON with the winning LawModel
	 * @throws LiquidoException for example when poll is not in correct status
	 */
	@RequestMapping(value = "/dev/polls/{pollId}/finishVotingPhase")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public @ResponseBody Lson devFinishVotingPhase(@PathVariable(name="pollId") PollModel poll) throws LiquidoException {
		if (poll == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Cannot find poll with that id");
		log.info("DEV: Finish voting phase of "+poll);
		LawModel winner = pollService.finishVotingPhase(poll);
		//TODO: cancel the scheduled Quartz trigger for starting the poll.
		return new Lson()
			.put("ok", "Finished voting phase of poll.id="+poll.id)
			.put("winner", winner);
	}

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;

	/**
	 * Completely delete poll and all casted ballots in this poll. Will NOT delete any proposals
	 * You <b>must be logged in as ADMIN</b> to delete a poll.
	 * @param poll an existing poll
	 * @return HTTP 200
	 */
	@RequestMapping(value = "/dev/polls/{pollId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public @ResponseBody Lson deletePoll(
		@PathVariable(name="pollId") PollModel poll,
		@RequestParam(name="deleteProposals") boolean deleteProposals
	) throws LiquidoException {
		UserModel currentAuditor = liquidoAuditorAware.getCurrentAuditor()
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Admin must be logged in to delete a poll."));
		log.info("DELETE " + poll + (deleteProposals ? " and all its proposals" : "") + " by "+currentAuditor.toStringShort());
		pollService.deletePoll(poll, deleteProposals);
		return new Lson()
			.put("ok", "Poll(id="+poll.id+") has been DELETED")
			.put("deleteProposals", deleteProposals);
	}



}
