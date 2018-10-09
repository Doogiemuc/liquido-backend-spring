package org.doogie.liquido.testdata;

/**
 * Fixed test data.
 * All tests may rely on the data that is created in {@link org.doogie.liquido.testdata.TestDataCreator}
 * These test fixtures are IDs, keys and attribute values that were created there.
 */
public class TestFixtures {

  public static final String USER1_EMAIL = "testuser1@liquido.de";      // testuser1 has  ID 1 in the DB !
  public static final String USER2_EMAIL = "testuser2@liquido.de";
  public static final String USER3_EMAIL = "testuser3@liquido.de";
  public static final String USER4_EMAIL = "testuser4@liquido.de";
  public static final String USER5_EMAIL = "testuser5@liquido.de";
	public static final String USER6_EMAIL = "testuser6@liquido.de";

  public static final String USER1_PWD   = "dummyPasswordHash";

	public static final String AREA0_TITLE = "Area 0";
  public static final String AREA1_TITLE = "Area 1";
  public static final long   USER4_NUM_VOTES = 5;     // testuser4@liquido.de  has 5 votes (including his own) due to (transitive) delegations

  //TODO: REST URLs should also be part of these TestFixtures. When an URL changes in the backend this should break a test.
}