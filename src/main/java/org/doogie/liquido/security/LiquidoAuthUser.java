package org.doogie.liquido.security;

import org.doogie.liquido.model.UserModel;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Adapter between my Liquido {@link UserModel} and Spring-securitie's {@link org.springframework.security.core.userdetails.User}
 */
public class LiquidoAuthUser extends User {
  private UserModel liquidoUserModel;

  public LiquidoAuthUser(String username, String password, Collection<? extends GrantedAuthority> authorities, UserModel liquidoUserModel) {
    super(username, password, authorities);
    this.liquidoUserModel = liquidoUserModel;
  }

  public UserModel getLiquidoUserModel() {
    return liquidoUserModel;
  }

  public void setLiquidoUserModel(UserModel userModel) {
    this.liquidoUserModel = userModel;
  }
}
