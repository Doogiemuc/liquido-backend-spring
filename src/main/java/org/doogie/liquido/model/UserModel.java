package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * One user / voter / citizen
 */
@Data
@EqualsAndHashCode(of="email", callSuper = true)    // Compare users by their uniquie e-mail  (and ID)
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "users")
public class UserModel extends BaseModel {
  @NotNull
  @NonNull
  @Column(unique = true)
  public String email;

  //@NotNull
  //@NonNull
  //JsonIgnore                      // tell jackson to not serialize this field  => needs more  CHI
  //@Getter(AccessLevel.PRIVATE)    // Getter cannot be private because I need access to the password in LiquidoUserDetailsService.java
  //@RestResource(exported = false)   // private: never exposed via REST!
  //private String passwordHash;


	@Embedded
	public UserProfileModel profile;

	/*
	 * get the Password.  Do not serialize this field with jackson
	 * @return

	@JsonIgnore
	public String getPasswordHash() {
		return passwordHash;
	}

	/*
	 * set passwordHash. This can be deserialized from JSON
	 * @param passwordHash

	@JsonProperty
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}
  */


  /* DEPRECATED: replaced by DelegationModel

     Yes it is possible ot model voter --(area)--> proxy  relation as a java.util.Map.
     But it's better to use a seperate Model in between. Especially when the link has attributes.
     See https://vladmihalcea.com/a-beginners-guide-to-jpa-and-hibernate-cascade-types/

	@ManyToMany
	@JoinTable(
			name="PROXIES",
			joinColumns={@JoinColumn(name="fromUser")},   // , referencedColumnName="id"
			inverseJoinColumns={@JoinColumn(name="toProxy")}
	)
	@MapKeyJoinColumn(name = "AREA_ID")  				// I love JPA:  @MapKey, @MapKeyColumn or @MapKeyJoinColumn  :-)
	Map<AreaModel, UserModel> proxyMap;
  */



  /* all the delegees that this proxy may vote for
  //DEPRECATED:   replaced by ChecksumModel     but nice example how to map a java.util.List in JPA
  @ElementCollection(fetch = FetchType.EAGER)  //BUGFIX: needed to prevent LazyInitializationException   http://stackoverflow.com/questions/22821695/lazyinitializationexception-failed-to-lazily-initialize-a-collection-of-roles
  @CollectionTable(name = "USERS_VOTER_TOKENS")
  @JsonIgnore  // do not expose externally
  @RestResource(exported = false)
  public List<String> voterTokens;
  */



	// This is extremely important! Do not expose password in toString() !!!
  @Override
  public String toString() {
  	StringBuffer buf = new StringBuffer();
    buf.append("UserModel[");
		buf.append("id=" + id);
		buf.append(", email='" + email + '\'');
		if (this.getProfile() != null) {
			buf.append(", profile.mobilephone=" + this.getProfile().getMobilephone());
			buf.append(", profile.picture=" + this.getProfile().getPicture());
		}
		buf.append(']');
		return buf.toString();
  }

}
