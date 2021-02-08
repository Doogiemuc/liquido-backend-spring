package org.doogie.liquido.test.testUtils;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This anotatin can be used on test methods to simulate a specific logged in user
 * You <em>must</em> provide either userId or email.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = TestSecurityContextFactory.class)
public @interface WithMockTeamUser {
	long userId() default -1;
	long teamId() default -1;
	String email() default "";
}