package org.doogie.liquido.testdata;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed test data.
 * All tests may rely on the data that is created in {@link org.doogie.liquido.testdata.TestDataCreator}
 * These test fixtures are IDs, keys and attribute values that were created there.
 */
public class TestFixtures {

	public static final String MAIL_PREFIX = "testuser";
	
  public static final String USER1_EMAIL = MAIL_PREFIX+"1@liquido.de";      // testuser1 has  ID 1 in the DB !
  public static final String USER2_EMAIL = MAIL_PREFIX+"2@liquido.de";
  public static final String USER3_EMAIL = MAIL_PREFIX+"3@liquido.de";
  public static final String USER4_EMAIL = MAIL_PREFIX+"4@liquido.de";
  public static final String USER5_EMAIL = MAIL_PREFIX+"5@liquido.de";
	public static final String USER6_EMAIL = MAIL_PREFIX+"6@liquido.de";

  public static final String TESTUSER_PASSWORD = "dummyPassword";  // password for all created users, that will be hashed

	public static final String AREA0_TITLE = "Area 0";
  public static final String AREA1_TITLE = "Area 1";

	public static List<String[]> delegations = new ArrayList<>();
	public static final String AREA_FOR_DELEGATIONS = AREA0_TITLE;
	public static final long   USER1_NUM_VOTES = 6;     // testuser1@liquido.de  has 6 votes (including his own) due to (transitive) delegations

	static {
		delegations.add(new String[]{TestFixtures.USER2_EMAIL, TestFixtures.USER1_EMAIL});   // testuser2 delegates to proxy testuser1
		delegations.add(new String[]{TestFixtures.USER3_EMAIL, TestFixtures.USER1_EMAIL});
		delegations.add(new String[]{TestFixtures.USER4_EMAIL, TestFixtures.USER1_EMAIL});
		delegations.add(new String[]{TestFixtures.USER5_EMAIL, TestFixtures.USER4_EMAIL});
		delegations.add(new String[]{TestFixtures.USER6_EMAIL, TestFixtures.USER4_EMAIL});
	}


  //TODO: REST URLs should also be part of these TestFixtures. When an URL changes in the backend this should break a test.
}