package org.doogie.liquido.security;

import lombok.NonNull;
import org.doogie.liquido.model.UserModel;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Adapter between my Liquido-{@link UserModel} and Spring-security's {@link org.springframework.security.core.userdetails.User}
 * The spring User can be retreived as the HTTP Principal. With this adapter you can get our custom Liuqido UserModel from that.
 */
public class LiquidoAuthUser extends User {  // org.springframework.security.core.userdetails.User implements UserDetails, CredentialsContainer
  private UserModel liquidoUserModel;

  /**
   * Create a new Liquido authentication user that contains a custom liquidoUserModel.
   * No password here!
   * @param username
   * @param authorities
   * @param liquidoUserModel
   */
  public LiquidoAuthUser(String username, Collection<? extends GrantedAuthority> authorities, @NonNull UserModel liquidoUserModel) {
    super(username, "", authorities);
    this.liquidoUserModel = liquidoUserModel;
  }

  public UserModel getLiquidoUserModel() {
  	return liquidoUserModel;
  }


  //TODO: This is just for testing
  @Override
  public String getPassword() {
    System.out.println("SEC TRACE: (LiquidoAuthUser.java:30): Some code is getting the password of "+this.getUsername());
    return super.getPassword();
  }
}
