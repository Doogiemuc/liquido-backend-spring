package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
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
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This controller is only available for development and testing
 */
@Slf4j
@RestController
@RequestMapping("${spring.data.rest.base-path}")    //TODO: add /dev prefix here!
//@Profile({"dev", "test"})			// For security reasons this controller should not be available in PROD. But I need it when I want to run tests against prod!
public class DevRestController {

	@Value(value = "${spring.data.rest.base-path}")
	String basePath;

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
	 * Get list of users for the quick login at the top right of the UI. Admin is first element (if configured)
	 * This endpoint must be public, because the web app needs it during very early application start. (See main.js) But client must at least provide devLoginToken.
	 * And this is only available in DEV and TEST environment!
	 * @param token dev login sms token
	 * @return UserModel HATEOAS resource with 10 users. Admin is first element.
	 */
	@Profile({"dev", "test"})
	@RequestMapping(value = "/dev/users")
	public @ResponseBody Lson devGetAllUsers(
		@RequestParam("token") String token,
		HttpServletRequest request
	) throws LiquidoException {
		log.debug("GET /dev/users");
		if (!token.equals(prop.devLoginToken))
			throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Invalid token for GET /dev/users");
		List<UserModel> users = new ArrayList<UserModel>();
		Optional<UserModel> adminOpt = userRepo.findByEmail(prop.admin.email);
		if (adminOpt.isPresent()) users.add(adminOpt.get());
		int count = 0;
		for (UserModel user : userRepo.findAll()) {
			if (!user.getEmail().equalsIgnoreCase(prop.admin.email)) users.add(user);   // don't add the admin twice
			count++;
			if (count > 10) break;
		}

		/* non of those work since this is a plain @RestController
		TemplateVariables variables = new TemplateVariables(new TemplateVariable("spring.data.rest.base-path", TemplateVariable.VariableType.PATH_VARIABLE));
		ControllerLinkBuilder controllerLinkBuilder = linkTo(methodOn(DevRestController.class, variables).devGetAllUsers(null, null));
		UriComponents build = UriComponentsBuilder.fromPath("/dev/users").build();
		ServletUriComponentsBuilder servletUriComponentsBuilder = ServletUriComponentsBuilder.fromCurrentRequestUri();
		return new Resources<>(users, linkTo(methodOn(DevRestController.class).devGetAllUsers(null)).withRel("self"));
		 */
		String selfUrl = request.getRequestURL().toString() + "?token={token}";   // let's do a quick hack to to create selfUrl

		return Lson.builder()
			.put("_embedded.users", users)  // Always return at least an empty array! Be nice to your clients!
			.put("_link.self.href", selfUrl);

	}

	/**
	 * Receive a Json Web Token for authentication. This for example allows tests to login as <b>any</b> user.
	 * Then authentication for further requests can then be handled normally in {@link org.doogie.liquido.security.LiquidoUserDetailsService#loadUserByUsername(String)}
	 *
	 * 	The client must still provide the valid devLoginToken from application.properties. This is also available in PROD for testing against PROD!
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
		if (!token.equals(prop.devLoginToken))
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
