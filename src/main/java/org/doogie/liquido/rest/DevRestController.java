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
import org.springframework.core.env.Profiles;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * This controller is only available for development and testing
 */
@Slf4j
@RestController
@RequestMapping("${spring.data.rest.base-path}")    //TODO: add /dev prefix here!
@Profile({"dev", "test"})			// This controller is only available in dev or test environment
//TODO: If I want to run automated tests against prod, then I also need this in PROD.  How to secure it?
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

	@RequestMapping(value = "/dev/users")
	// This must be public, because during DEV I need this without beeing logged in.
	public @ResponseBody Resources<UserModel> devGetAllUsers()  {
		return new Resources<>(userRepo.findAll(), linkTo(methodOn(DevRestController.class).devGetAllUsers()).withRel("self"));
	}

	/**
	 * Allow login as any user.  (Only in DEV and TEST environment. And client must still provide valid devLoginSmsToken from application.properties
	 * @param mobile mobilephone of existing user
	 * @param token  devLoginSmsToken from application.properties
	 * @return Json Web Token for this user
	 * @throws LiquidoException when token is invalid or no user with that mobile is phone
	 */
	@RequestMapping(path = "/dev/loginWithSmsToken", produces = MediaType.TEXT_PLAIN_VALUE)
	public String loginWithSmsToken(
		@RequestParam("mobile") String mobile,
		@RequestParam("token") String token
	) throws LiquidoException {
		log.info("Dev Login mobile"+mobile);
		if (!token.equals(prop.devLoginSmsToken))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Dev Login failed. Token for mobile="+mobile+" is invalid!");
		UserModel user = userRepo.findByProfileMobilephone(mobile)
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "DevLogin: user for mobile phone " + mobile + " not found."));
		log.debug("DEV Login as " + mobile + " => " + user);
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
	public @ResponseBody Lson deletePoll(@PathVariable(name="pollId") PollModel poll) throws LiquidoException {
		UserModel currentAuditor = liquidoAuditorAware.getCurrentAuditor()
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Admin must be logged in to delete a poll."));
		log.info("DELETE "+poll+ " by "+currentAuditor.toStringShort());
		pollService.deletePoll(poll);
		return new Lson("ok", "Poll(id="+poll.id+") has been DELETED");
	}


}
