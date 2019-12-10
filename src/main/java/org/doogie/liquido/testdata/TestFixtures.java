package org.doogie.liquido.testdata;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed test data.
 * All tests may rely on the data that is created in {@link TestDataCreator}
 * These test fixtures are IDs, keys and attribute values that were created there.
 */
public class TestFixtures {

	public static final int NUM_USERS = 20;
	public static final int NUM_VOTES = 15;			// number of casted votes.  Must be smaller than NUM_USERS!
	public static final int NUM_AREAS = 10;
	public static final int NUM_IDEAS = 111;
	public static final int NUM_PROPOSALS = 50;
	public static final int NUM_ALTERNATIVE_PROPOSALS = 5;   // proposals in poll


	public static final String AREA0_TITLE = "Area 0";
	public static final String AREA1_TITLE = "Area 1";

	public static final String MAIL_PREFIX = "testuser";
	public static final String MOBILEPHONE_PREFIX = "+4912345";

	public static final String AVATAR_PREFIX = "/static/img/avatars/Avatar";
	public static final String USER1_NAME = "Donald Duck";
  public static final String USER1_EMAIL = MAIL_PREFIX+"1@liquido.de";      // testuser1 has  ID 1 in the DB !
  public static final String USER2_EMAIL = MAIL_PREFIX+"2@liquido.de";
  public static final String USER3_EMAIL = MAIL_PREFIX+"3@liquido.de";
  public static final String USER4_EMAIL = MAIL_PREFIX+"4@liquido.de";
  public static final String USER5_EMAIL = MAIL_PREFIX+"5@liquido.de";
	public static final String USER6_EMAIL = MAIL_PREFIX+"6@liquido.de";
	public static final String USER7_EMAIL = MAIL_PREFIX+"7@liquido.de";
	public static final String USER8_EMAIL = MAIL_PREFIX+"7@liquido.de";
	public static final String USER9_EMAIL = MAIL_PREFIX+"9@liquido.de";
	public static final String USER10_EMAIL = MAIL_PREFIX+"10@liquido.de";
	public static final String USER11_EMAIL = MAIL_PREFIX+"11@liquido.de";
	public static final String USER12_EMAIL = MAIL_PREFIX+"12@liquido.de";

	// Ideas
	public static final String IDEA_0_TITLE = "Idea 0 title from TestFixtures";

	// Laws
	public static final  int NUM_LAWS = 2;
	public static final String LAW_TITLE = "Law 1";

  /**
   * This secret is used when a test needs to create a voter token
   * This must only be used in tests!
   */
	public static final String USER_TOKEN_SECRET = "userTokenSecret";


	/** dynamic method as TestFixture.  Mmhh nice! */
	public static boolean shouldBePublicProxy(AreaModel area, UserModel voter) {
		return !USER2_EMAIL.equals(voter.getEmail());  // everyone except user2 is a public proxy
	}


	/* Example data for delegations:

	                            				    user1          <---- top Proxy
        					                     /    |    \
  user2 is not a public proxy --> (user2) user3  user4
  delegation request 5-> 2    -->  /R/      |       \
      		                       user5    user6    user7
      		                      /    \           /   |  (\)    <---- 12->7  non-transitive delegation
      		                 user8    user9  user10 user11 user12

 └── UserModel[id=1, email='testuser1@liquido.de', profile.mobilephone=+49123451, profile.picture=/static/img/photos/1.png]
     ├── UserModel[id=2, email='testuser2@liquido.de', profile.mobilephone=+49123452, profile.picture=/static/img/photos/2.png]
     │   └── UserModel[id=5, email='testuser5@liquido.de', profile.mobilephone=+49123455, profile.picture=/static/img/photos/2.png]
     ├── UserModel[id=3, email='testuser3@liquido.de', profile.mobilephone=+49123453, profile.picture=/static/img/photos/3.png]
     │   └── UserModel[id=6, email='testuser6@liquido.de', profile.mobilephone=+49123456, profile.picture=/static/img/photos/3.png]
     └── UserModel[id=4, email='testuser4@liquido.de', profile.mobilephone=+49123454, profile.picture=/static/img/photos/1.png]
         └── UserModel[id=7, email='testuser7@liquido.de', profile.mobilephone=+49123457, profile.picture=/static/img/photos/1.png]
             ├── UserModel[id=10, email='testuser10@liquido.de', profile.mobilephone=+491234510, profile.picture=/static/img/photos/1.png]
             ├── UserModel[id=11, email='testuser11@liquido.de', profile.mobilephone=+491234511, profile.picture=/static/img/photos/2.png]
             └── UserModel[id=12, email='testuser12@liquido.de', profile.mobilephone=+491234512, profile.picture=/static/img/photos/3.png]



	 */
  // This test data must match RestEndpointTests.testGetProxyMap()  and ProxyServiceTests.testGetNumVotes !!!
  public static List<String[]> delegations = new ArrayList<>();
	public static final String AREA_FOR_DELEGATIONS = AREA0_TITLE;
	public static final String TOP_PROXY_EMAIL = USER1_EMAIL;
	public static final int USER1_DELEGATIONS = 7;     // testuser1@liquido.de  has 7 delegations to him (the non-transitive one 12-7 is not counted for him!)
	public static final int USER2_DELEGATIONS = 0;		 // testuser2@liquido.de  has a requested delegation. He has no accepted delegations yet.
	public static final int USER4_DELEGATIONS = 3;     // testuser4@liquido.de  has 3 direct delegations
	static {
		// fromUser, toProxy, transitive?
		delegations.add(new String[]{TestFixtures.USER2_EMAIL, TestFixtures.USER1_EMAIL, "true"});   // testuser2 delegates to proxy testuser1
		delegations.add(new String[]{TestFixtures.USER3_EMAIL, TestFixtures.USER1_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER4_EMAIL, TestFixtures.USER1_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER5_EMAIL, TestFixtures.USER2_EMAIL, "true"});  // 5 -> 2 requested, because user2 is not a public proxy
		delegations.add(new String[]{TestFixtures.USER6_EMAIL, TestFixtures.USER3_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER7_EMAIL, TestFixtures.USER4_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER10_EMAIL, TestFixtures.USER7_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER11_EMAIL, TestFixtures.USER7_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER12_EMAIL, TestFixtures.USER7_EMAIL, "false"});  // 12 -> 7  non transitive
	}


  //TODO: REST URLs should also be part of these TestFixtures. When an URL changes in the backend this SHOULD break a test.
}