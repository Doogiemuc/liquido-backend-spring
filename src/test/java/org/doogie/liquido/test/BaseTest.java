package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.PostConstruct;
import java.util.Random;

/**
 * Base for all test classes.
 * Provides basic utility functions that all tests need.
 * See {@link HttpBaseTest} for HTTP related stuff.
 */
@Slf4j
@ActiveProfiles(value={"test","local"})			// Activate spring profile "test" and "local". Needed when run via maven. I am wondering why "test" is not the default?
public class BaseTest {
	@Autowired
	LiquidoAuditorAware auditor;

	@Autowired
	UserRepo userRepo;

	@Autowired
	LiquidoProperties props;

	static Random rand = new Random(System.currentTimeMillis());

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	TeamRepo teamRepo;

	@Autowired
	AuthUtil authUtil;

	/* default area is lazily initiated in getDefaultArea() */
	private AreaModel defaultArea = null;

	/** all test cases will run against this team */
	public TeamModel team;

	@PostConstruct
	public void loadTeamTestee() {
		// TODO: refactor this as lazy loading
		this.team = teamRepo.findAll(new OffsetLimitPageable(0, 1)).iterator().next();
	}

	/**
	 * Lazily load the default area from the default. See liquido.defaultAreaTitle in application.properties
	 * @return the default area
	 * @throws RuntimeException when there is no area with that title
	 */
	public AreaModel getDefaultArea() throws RuntimeException {
		if (this.defaultArea == null) {
			this.defaultArea = areaRepo.findByTitle(props.getDefaultAreaTitle()).orElseThrow(
				() -> new RuntimeException("Cannot find default area")
			);
		}
		return this.defaultArea;
	}


	/**
	 * Entry and exit logging for <b>all</b> test cases. Jiipppiiee. Did I already mention that I am a logging fanatic *G*
	 */
	/*
	//TODO: @ExtendWith(
	public TestWatcher slf4jTestWatcher = new TestWatcher() {
		@Override
		protected void starting(Description descr) {
			log.debug("===== TEST STARTING "+descr.getDisplayName());
		}

		@Override
		protected void failed(Throwable e, Description descr) {
			log.error("===== TEST FAILED "+descr.getDisplayName()+ ": "+e.toString());
		}

		@Override
		protected void succeeded(Description descr) {
			log.debug("===== TEST SUCCESS "+descr.getDisplayName());
		}
	};

	 */


	/**
	 * Login a user into spring's SecurityContext for testing
	 * @param email email of an existing user
	 * @throws UsernameNotFoundException when email is not found in the DB.
	 * @throws LiquidoException when

	public void loginMockUser(String email, Long teamId) throws LiquidoException {
		log.debug("TEST loginUser("+email+")");

		// create a dummy auth token
		UserModel user = userRepo.findByEmail(email).orElseThrow(
			LiquidoException.notFound("Cannot find user with email "+email+ " to loginMockUser()")
		);
		authUtil.authenticateInSecurityContext(user.getId(), teamId);

		// Also set as mock auditor for createdBy
		auditor.setMockAuditor(user);
	}
	*/

	private static final String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, nam urna. Vitae aenean velit, voluptate velit rutrum. Elementum integer rhoncus rutrum morbi aliquam metus, morbi nulla, nec est phasellus dolor eros in libero. Volutpat dui feugiat, non magna, parturient dignissim lacus ipsum in adipiscing ut. Et quis adipiscing perferendis et, id consequat ac, dictum dui fermentum ornare rhoncus lobortis amet. Eveniet nulla sollicitudin, dolore nullam massa tortor ullamcorper mauris. Lectus ipsum lacus.\n" +
		"Vivamus placerat a sodales est, vestibulum nec cursus eros fermentum. Felis orci nunc quis suspendisse dignissim justo, sed proin metus, nunc elit ac aliquam. Sed tellus ante ipsum erat platea nulla, enim bibendum gravida condimentum, imperdiet in vitae faucibus ultrices, aenean fringilla at. Rhoncus et sint volutpat, bibendum neque arcu, posuere viverra in, imperdiet duis. Eget erat condimentum congue ipsam. Tortor nostra, adipiscing facilisis donec elit pellentesque natoque integer. Ipsum id. Aenean suspendisse et eros hymenaeos in auctor, porttitor amet id pellentesque tempor, praesent aliquam rhoncus convallis vel, tempor fusce wisi enim aliquam ut nisl, nullam dictum etiam. Nisi accumsan augue sapiente dui, pulvinar cras sapien mus quam nonummy vivamus, in vitae, sociis pede, convallis mollis id mauris. Vestibulum ac quis scelerisque magnis pede in, duis ullamcorper a ipsum ante ornare.\n" +
		"Quam amet. Risus lorem nibh consequat volutpat. Bibendum lorem, mauris sed quisque. Pellentesque augue eros nibh, iaculis maecenas facilisis amet. Nam laoreet elit litora justo, morbi in vitae nisl nulla vestibulum maecenas. Scelerisque lacinia id eget pede nunc in, id a nullam nunc velit mauris class. Duis dui ullamcorper vestibulum, turpis mi eu, arcu pellentesque sit. Arcu nibh elit. Vitae magna magna auctor, class pariatur, tortor eget amet mi pede accumsan, ut quam ut ante nibh vivamus quisque. Magna praesent tortor praesent.";

	/** @return a dummy text that can be used eg. in descriptions */
	public static String getLoremIpsum(int minLength, int maxLength) {
		int endIndex = minLength + rand.nextInt(maxLength - minLength);
		if (endIndex >= loremIpsum.length()) endIndex = loremIpsum.length()-1;
		return loremIpsum.substring(0, endIndex);
	}
}
