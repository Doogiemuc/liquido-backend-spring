package org.doogie.liquido.security;

import org.doogie.liquido.model.UserModel;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Adapter between my [@link {@link UserModel} and Spring-securitie's {@link org.springframework.security.core.userdetails.User}
 *
 */
public class LiquidoAuthUser extends User {
  private UserModel userModel;

  public LiquidoAuthUser(String username, String password, Collection<? extends GrantedAuthority> authorities, UserModel userModel) {
    super(username, password, authorities);
    this.userModel = userModel;
  }

  public UserModel getUserModel() {
    return userModel;
  }

  public void setUserModel(UserModel userModel) {
    this.userModel = userModel;
  }
}
