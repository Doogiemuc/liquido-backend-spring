package org.doogie.liquido.testdata;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed test data.
 * All tests may rely on the data that is created in {@link org.doogie.liquido.testdata.TestDataCreator}
 * These test fixtures are IDs, keys and attribute values that were created there.
 */
public class TestFixtures {

	public static final int NUM_USERS = 20;
	public static final int NUM_VOTES = 15;			// number of casted votes.  Must be smaller than NUM_USERS!
	public static final  int NUM_AREAS = 10;
	public static final int NUM_IDEAS = 111;
	public static final  int NUM_PROPOSALS = 50;
	public static final  int NUM_ALTERNATIVE_PROPOSALS = 5;   // proposals in poll
	public static final  int NUM_LAWS = 2;

	public static final String AREA0_TITLE = "Area 0";
	public static final String AREA1_TITLE = "Area 1";

	public static final String MAIL_PREFIX = "testuser";
  public static final String USER1_EMAIL = MAIL_PREFIX+"1@liquido.de";      // testuser1 has  ID 1 in the DB !
  public static final String USER2_EMAIL = MAIL_PREFIX+"2@liquido.de";
  public static final String USER3_EMAIL = MAIL_PREFIX+"3@liquido.de";
  public static final String USER4_EMAIL = MAIL_PREFIX+"4@liquido.de";
  public static final String USER5_EMAIL = MAIL_PREFIX+"5@liquido.de";
	public static final String USER6_EMAIL = MAIL_PREFIX+"6@liquido.de";
	public static final String USER7_EMAIL = MAIL_PREFIX+"7@liquido.de";

	/* p
  /** this secret is used when user requests a voter token */
	public static final String USER_TOKEN_SECRET = "userTokenSecret";


	/** dynamic method as TestFixture.  Mmhh nice! */
	public static boolean shouldBePublicProxy(AreaModel area, UserModel voter) {
		return !USER2_EMAIL.equals(voter.getEmail());  // everyone except user2 is a public proxy
	}


	/* Example data for delegations:

	                     					user1          <---- top Proxy
					                    /   |   \
     no public proxy ---> user2  user3  user4
	delegation request ->  /R/      |      (\)   <---- 6->4  non-transitive        7->2: requested because user2 is not a public proxy
		                   user7		user5    user6
	 */
  // This test data must match RestEndpointTests.testGetProxyMap()  and ProxyServiceTests.testGetNumVotes !!!
  public static List<String[]> delegations = new ArrayList<>();
	public static final String AREA_FOR_DELEGATIONS = AREA0_TITLE;
	public static final String TOP_PROXY_EMAIL = USER1_EMAIL;
	public static final long   USER1_NUM_VOTES = 5;     // testuser1@liquido.de  has 5 votes (including his own) due to (transitive) delegations
	public static final long   USER4_NUM_VOTES = 3;     // testuser4@liquido.de  has 3 votes (including his own) due to direct delegations
	static {
		// fromUser, toProxy, transitive?
		delegations.add(new String[]{TestFixtures.USER2_EMAIL, TestFixtures.USER1_EMAIL, "true"});   // testuser2 delegates to proxy testuser1
		delegations.add(new String[]{TestFixtures.USER3_EMAIL, TestFixtures.USER1_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER4_EMAIL, TestFixtures.USER1_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER5_EMAIL, TestFixtures.USER4_EMAIL, "true"});
		delegations.add(new String[]{TestFixtures.USER6_EMAIL, TestFixtures.USER4_EMAIL, "false"});  // 6 -> 4  is non-transitive
		delegations.add(new String[]{TestFixtures.USER7_EMAIL, TestFixtures.USER2_EMAIL, "false"});  // 7 -> 2  requested delegation
	}


  //TODO: REST URLs should also be part of these TestFixtures. When an URL changes in the backend this should break a test.
}