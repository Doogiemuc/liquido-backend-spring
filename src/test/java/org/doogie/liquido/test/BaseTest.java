package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.security.LiquidoAuthUser;
import org.doogie.liquido.security.LiquidoUserDetailsService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.test.testUtils.JwtAuthInterceptor;
import org.doogie.liquido.test.testUtils.LogClientRequestInterceptor;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base for all test classes.
 * Provides many utility functions,
 * an anonymous HTTP client,
 * and a logged in HTTP client
 */
@Slf4j
@ActiveProfiles("test")			// Activate spring profile "test". Needed when run via maven. I am wondergin why this is not the default
public class BaseTest {
	@Autowired
	LiquidoAuditorAware auditor;

	@Autowired
	UserRepo userRepo;

	@Autowired
	LiquidoUserDetailsService liquidoUserDetailsService;

	@Autowired
	LiquidoProperties props;

	@Autowired
	AreaRepo areaRepo;

	private AreaModel defaultArea = null;

	/**
	 * Entry and exit logging for <b>all</b> test cases. Jiipppiiee. Did I already mention that I am a logging fanatic *G*
	 */
	@Rule
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
	 * Login a user into spring's SecurityContext for testing
	 * @param email email of an existing user
	 * @throws UsernameNotFoundException when email is not found in the DB.
	 * @throws LiquidoException when
	 */
	public void loginUser(String email) throws LiquidoException {
		log.debug("TEST loginUser("+email+")");
		LiquidoAuthUser userDetails;
		try {
			userDetails = liquidoUserDetailsService.loadUserByUsername(email);
		} catch(UsernameNotFoundException e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Cannot login testuser. email="+email, e);
		}
		List<GrantedAuthority> authList = userDetails.getAuthorities().stream().collect(Collectors.toList());
		Authentication authentication = new TestingAuthenticationToken(userDetails, null, authList);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// also log in as mock auditor
		UserModel admin = userRepo.findByEmail(email)
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Cannot login test user. email="+email));
		auditor.setMockAuditor(admin);
	}
}
