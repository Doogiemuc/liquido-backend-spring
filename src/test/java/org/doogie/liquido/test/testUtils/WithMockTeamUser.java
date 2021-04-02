package org.doogie.liquido.test.testUtils;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

/**
 * This annotation can be used on test methods to simulate a specific logged in user
 * <b>You <em>must</em> provide either userId or email.</b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@WithSecurityContext(factory = TestSecurityContextFactory.class)
public @interface WithMockTeamUser {
	long userId() default -1;
	String email() default "";
	long teamId() default -1;  // TODO: make team mandatory. A user can only be authenticated IN a team.
}