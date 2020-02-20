package org.doogie.liquido.test.matchers;

import org.doogie.liquido.model.UserModel;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** Hamcrest matcher that matches a user by his email. */
public class UserMatcher extends TypeSafeMatcher<UserModel> {
  private String email;

  public UserMatcher(String email) {
    this.email = email;
  }

  @Override
  protected boolean matchesSafely(UserModel userModel) {
    return email.equals(userModel.getEmail());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("user '"+email+"' not found");
  }

  public static Matcher userWithEMail(String email) { return new UserMatcher(email); }
}
