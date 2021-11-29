package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * AuditorAware implementation that returns the currently logged in user.
 * This is for example used to fill @CreatedBy attributes in models.
 * It can also set and return a mocked auditor. (This is used in tests.)
 */
@Slf4j
@Component
public class LiquidoAuditorAware implements AuditorAware<UserModel> {

	@Autowired
	Environment springEnv;

	@Autowired
  AuthUtil authUtil;

  UserModel mockAuditor = null;

  /**
   * Get the auditor that will be set as createdBy for newly created JPA entities.
	 * @see AuthUtil#getCurrentUserFromDB()
   * @return (An Java optional that resolves to) the currently logged in user as a liquido UserModel
   */
  @Override
  public Optional<UserModel> getCurrentAuditor() {
		if (mockAuditor != null) {
			// warn about mock users, but only if we are not in dev, test or int
			if (!springEnv.acceptsProfiles(Profiles.of("dev", "test", "int")))
				log.warn("Returning mock auditor " + mockAuditor.getEmail());
			return Optional.of(mockAuditor);
		}
		return authUtil.getCurrentUserFromDB();
	}

  public void setMockAuditor(UserModel mockAuditor) {
    if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
      log.debug("Setting mockauditor to "+mockAuditor);  // mockauditor may be null!!!
    }
    this.mockAuditor = mockAuditor;
  }

}
