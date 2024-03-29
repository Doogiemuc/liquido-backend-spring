package org.doogie.liquido.testdata;

import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed test data.
 * {@link TestDataCreator} will create DB entities as defined here.
 * All JUnit Test Cases may then simply rely on this pre-created models.
 */
public class TestFixtures {

	public static final int NUM_TEAMS = 3;
	public static final int NUM_TEAM_MEMBERS = 5;
	public static final int NUM_USERS = 100;    // need 100 users for PollServiceTest.testRankedPairs()
	public static final int NUM_VOTES = 15;			// number of casted votes. Must be smaller than NUM_USERS!
	public static final int NUM_AREAS = 10;
	public static final int NUM_IDEAS = 111;
	public static final int NUM_PROPOSALS = 50;
	public static final int NUM_ALTERNATIVE_PROPOSALS = 5;   // proposals in poll

	public static final String AREA1_TITLE = "Area 1";

	// all email adresses MUST be lowercase here! Because they will be stored lowercase!!!
	public static final String USER_NAME_PREFIX = "test_";
	public static final String MAIL_PREFIX = "test_";
	public static final String MOBILEPHONE_PREFIX = "+49151555";
	public static final String DEFAULT_WEBSITE = "www.liquido.vote";
	public static final String EMAIL_DOMAIN = "liquido.vote";
	public static final String AVATAR_IMG_PREFIX = "Avatar";   // path to avatar images is configured in the client

	public static final String USER1_NAME = USER_NAME_PREFIX+"Donald Duck";				 // Special name for user1

  public static final String USER1_EMAIL = MAIL_PREFIX+"1@" + EMAIL_DOMAIN;      // testuser1 has ID 1 in the DB !
  public static final String USER2_EMAIL = MAIL_PREFIX+"2@" + EMAIL_DOMAIN;
  public static final String USER3_EMAIL = MAIL_PREFIX+"3@" + EMAIL_DOMAIN;
  public static final String USER4_EMAIL = MAIL_PREFIX+"4@" + EMAIL_DOMAIN;
  public static final String USER5_EMAIL = MAIL_PREFIX+"5@" + EMAIL_DOMAIN;
	public static final String USER6_EMAIL = MAIL_PREFIX+"6@" + EMAIL_DOMAIN;
	public static final String USER7_EMAIL = MAIL_PREFIX+"7@" + EMAIL_DOMAIN;
	public static final String USER8_EMAIL = MAIL_PREFIX+"7@" + EMAIL_DOMAIN;
	public static final String USER9_EMAIL = MAIL_PREFIX+"9@" + EMAIL_DOMAIN;
	public static final String USER10_EMAIL = MAIL_PREFIX+"10@" + EMAIL_DOMAIN;
	public static final String USER11_EMAIL = MAIL_PREFIX+"11@" + EMAIL_DOMAIN;
	public static final String USER12_EMAIL = MAIL_PREFIX+"12@" + EMAIL_DOMAIN;
	public static final String USER13_EMAIL = MAIL_PREFIX+"13@" + EMAIL_DOMAIN;
	public static final String USER14_EMAIL = MAIL_PREFIX+"14@" + EMAIL_DOMAIN;
	public static final String USER15_EMAIL = MAIL_PREFIX+"15@" + EMAIL_DOMAIN;
	public static final String USER16_EMAIL = MAIL_PREFIX+"16@" + EMAIL_DOMAIN;

	//TODO: create these email adresses dynamically and replace testUtils

	// Teams and members and admins
	public static final String TEAM1_NAME = "test_TeamXCCv33D";         													// Fixed TeamName that is used in tests
	public static final String TEAM1_ADMIN_EMAIL = MAIL_PREFIX + "adminofoneteam@"+EMAIL_DOMAIN;            // This user is admin in ONE team
	public static final String TEAM1_ADMIN_MOBILEPHONE = MOBILEPHONE_PREFIX+"111";
	public static final String TWO_TEAM_USER_EMAIL = MAIL_PREFIX + "memberintwoteams@"+EMAIL_DOMAIN;        // This user is member in TWO teams
	public static final String TWO_TEAM_USER_MOBILEPHONE = MOBILEPHONE_PREFIX+"222";
	public static final String TEAM_NAME_PREFIX = "test_Team";
	public static final String TEAM_ADMIN_NAME_PREFIX = USER_NAME_PREFIX + "Admin";
	public static final String TEAM_ADMIN_EMAIL_PREFIX = MAIL_PREFIX + "admin";  					// e.g. test_User_admin4711@TestTeam0.org
	public static final String TEAM_MEMBER_NAME_PREFIX = USER_NAME_PREFIX + "Member";
	public static final String TEAM_MEMBER_EMAIL_PREFIX = MAIL_PREFIX + "member";

	// Ideas
	public static final String IDEA_0_TITLE = "Idea 0 title from TestFixtures";

	// Laws
	public static final  int NUM_LAWS = 2;


  /**
   * This secret is used when a test needs to create a voter token
   * This must only be used in tests!
   */
	public static final String 	USER_TOKEN_SECRET = "test_userTokenSecret";


	/** dynamic method as TestFixture.  Mmhh nice! */
	public static boolean shouldBePublicProxy(AreaModel area, UserModel voter) {
		return !USER2_EMAIL.equals(voter.getEmail()) &&					// every normal user except user2 is a public proxy
			  !voter.getEmail().startsWith(TEAM_ADMIN_EMAIL_PREFIX) &&  // admins and members of teams are no public proxies
				!voter.getEmail().startsWith(TEAM_MEMBER_EMAIL_PREFIX);
	}


	/* Example data for delegations:

	                            				    user1          <---- top Proxy
        					                     /    |    \
  user2 is not a public proxy --> (user2) user3  user4
  delegation request 5-> 2    -->  /R/      |       \
      		                       user5    user6    user7
      		                      /    \           /   |   \
      		                 user8    user9  user10 user11 user12

DEBUG .(TestDataCreator.java:230).run()                                      | ====== TestDataCreator: Proxy tree =====
├─ UserModel[id=1, email='testuser1@liquido.de', profile.mobilephone=+49123451, profile.picture=/static/img/avatars/Avatar1.png]
│  ├─ UserModel[id=2, email='testuser2@liquido.de', profile.mobilephone=+49123452, profile.picture=/static/img/avatars/Avatar2.png]
│  │  └─ UserModel[id=5, email='testuser5@liquido.de', profile.mobilephone=+49123455, profile.picture=/static/img/avatars/Avatar5.png]
│  ├─ UserModel[id=3, email='testuser3@liquido.de', profile.mobilephone=+49123453, profile.picture=/static/img/avatars/Avatar3.png]
│  │  └─ UserModel[id=6, email='testuser6@liquido.de', profile.mobilephone=+49123456, profile.picture=/static/img/avatars/Avatar6.png]
│  └─ UserModel[id=4, email='testuser4@liquido.de', profile.mobilephone=+49123454, profile.picture=/static/img/avatars/Avatar4.png]
│     └─ UserModel[id=7, email='testuser7@liquido.de', profile.mobilephone=+49123457, profile.picture=/static/img/avatars/Avatar7.png]
│        ├─ UserModel[id=10, email='testuser10@liquido.de', profile.mobilephone=+491234510, profile.picture=/static/img/avatars/Avatar10.png]
│        ├─ UserModel[id=11, email='testuser11@liquido.de', profile.mobilephone=+491234511, profile.picture=/static/img/avatars/Avatar11.png]
│        └─ UserModel[id=12, email='testuser12@liquido.de', profile.mobilephone=+491234512, profile.picture=/static/img/avatars/Avatar12.png]

DEBUG .(TestDataCreator.java:233).run()                                      | ====== TestDataCreator: Tree of delegations =====
├─ DelegationModel[id=null, area=Area 0(id=22), fromUser=UserModel[id=1, email='testuser1@liquido.de'], toProxy=UserModel[id=null, email='aboveTopProxy@dummy.org'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│  ├─ DelegationModel[id=226, area=Area 0(id=22), fromUser=UserModel[id=2, email='testuser2@liquido.de'], toProxy=UserModel[id=1, email='testuser1@liquido.de'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│  │  └─ DelegationModel[id=229, area=Area 0(id=22), fromUser=UserModel[id=5, email='testuser5@liquido.de'], toProxy=UserModel[id=2, email='testuser2@liquido.de'], transitive=true, requestedDelegationFromChecksum=RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuNX3Ud6yFocoawh8dxtXS6Xinvh2wN8O, transitive=true, publicProxy=UserModel[id=5, email='testuser5@liquido.de'], delegatedTo.checksum=<null>], requestedDelegationAt=2019-12-16T11:10:32.106]
│  ├─ DelegationModel[id=227, area=Area 0(id=22), fromUser=UserModel[id=3, email='testuser3@liquido.de'], toProxy=UserModel[id=1, email='testuser1@liquido.de'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│  │  └─ DelegationModel[id=230, area=Area 0(id=22), fromUser=UserModel[id=6, email='testuser6@liquido.de'], toProxy=UserModel[id=3, email='testuser3@liquido.de'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│  └─ DelegationModel[id=228, area=Area 0(id=22), fromUser=UserModel[id=4, email='testuser4@liquido.de'], toProxy=UserModel[id=1, email='testuser1@liquido.de'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│     └─ DelegationModel[id=231, area=Area 0(id=22), fromUser=UserModel[id=7, email='testuser7@liquido.de'], toProxy=UserModel[id=4, email='testuser4@liquido.de'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│        ├─ DelegationModel[id=232, area=Area 0(id=22), fromUser=UserModel[id=10, email='testuser10@liquido.de'], toProxy=UserModel[id=7, email='testuser7@liquido.de'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│        ├─ DelegationModel[id=233, area=Area 0(id=22), fromUser=UserModel[id=11, email='testuser11@liquido.de'], toProxy=UserModel[id=7, email='testuser7@liquido.de'], transitive=true, requestedDelegationFromChecksum=null, requestedDelegationAt=null]
│        └─ DelegationModel[id=234, area=Area 0(id=22), fromUser=UserModel[id=12, email='testuser12@liquido.de'], toProxy=UserModel[id=7, email='testuser7@liquido.de'], transitive=false, requestedDelegationFromChecksum=null, requestedDelegationAt=null]

DEBUG .(TestDataCreator.java:237).run()                                      | ====== TestDataCreator: RightToVotes =====
DEBUG .(CastVoteService.java:88).createVoterTokenAndStoreRightToVote()       | createVoterTokenAndStoreRightToVote: for UserModel[id=1, email='testuser1@liquido.de', profile.mobilephone=+49123451, profile.picture=/static/img/avatars/Avatar1.png] in AreaModel[title='Area 0', id=22], becomePublicProxy=false
├─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuccBk8DrXNh0KtYIUb4eNp.t52wOn6W2, transitive=true, publicProxy=UserModel[id=1, email='testuser1@liquido.de'], delegatedTo.checksum=<null>]
│  ├─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuL7lV2QCtsOcieHmoCH6Frx3m0Y2gHDy, transitive=true, publicProxy=<null>, delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuccBk8DrXNh0KtYIUb4eNp.t52wOn6W2]
│  ├─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuEtxLCwrNT9pKlY1rgxYhl3RtaNJEQW., transitive=true, publicProxy=UserModel[id=3, email='testuser3@liquido.de'], delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuccBk8DrXNh0KtYIUb4eNp.t52wOn6W2]
│  │  └─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzulyuC.ir27zjCKynm/ef4Gp77Z9XU9UC, transitive=true, publicProxy=UserModel[id=6, email='testuser6@liquido.de'], delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuEtxLCwrNT9pKlY1rgxYhl3RtaNJEQW.]
│  └─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuZjktzIdpQANYKKzwjQ2AG3JLRied3ZC, transitive=true, publicProxy=UserModel[id=4, email='testuser4@liquido.de'], delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuccBk8DrXNh0KtYIUb4eNp.t52wOn6W2]
│     └─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzu4HeueVNdaHPs3H5dOkHoKyKmOv2LOTy, transitive=true, publicProxy=UserModel[id=7, email='testuser7@liquido.de'], delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuZjktzIdpQANYKKzwjQ2AG3JLRied3ZC]
│        ├─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuYBfqBJ8KRURXGBEFD1NIy8BIUtxNH8i, transitive=true, publicProxy=UserModel[id=10, email='testuser10@liquido.de'], delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzu4HeueVNdaHPs3H5dOkHoKyKmOv2LOTy]
│        ├─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzuj.6MAo8TWBWCw/Az7uRlus4mRiLJh/i, transitive=true, publicProxy=UserModel[id=11, email='testuser11@liquido.de'], delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzu4HeueVNdaHPs3H5dOkHoKyKmOv2LOTy]
│        └─ RightToVote[hashedVoterToken=$2a$10$1IdrGrRAN2Wp3U7QI.JIzukLk9n0nP/KZdEWe.TYbRMKT7d5wS9rm, transitive=false, publicProxy=UserModel[id=12, email='testuser12@liquido.de'], delegatedTo.checksum=$2a$10$1IdrGrRAN2Wp3U7QI.JIzu4HeueVNdaHPs3H5dOkHoKyKmOv2LOTy]

	 */


  // This test data must match RestEndpointTests.testGetProxyMap()  and ProxyServiceTests.testGetNumVotes !!!
  public static List<String[]> delegations = new ArrayList<>();
	public static final String TOP_PROXY_EMAIL = USER1_EMAIL;
	// Number of delegations.  (without the voter's own one. This would be voteCount)
	public static final long USER1_DELEGATIONS = 8;   		// testuser1@liquido.de  has 8 delegations to him (5 -> 2 is still only requested)
	public static final long USER2_DELEGATIONS = 0;				// testuser2@liquido.de  has a requested delegation. He has no accepted delegations yet.
	public static final long USER4_DELEGATIONS = 4;   		// testuser4@liquido.de  has 4 delegations from below him
	public static final long USER1_VOTE_COUNT_WHEN_USER4_VOTED = 3;   // When top proxy user1 votes while user4 has already voted, then his ballot counts 3 times.
	static {
		// Delegations  fromUser -> toProxy
		delegations.add(new String[]{TestFixtures.USER2_EMAIL, TestFixtures.USER1_EMAIL, });  // testuser2 delegates to proxy testuser1
		delegations.add(new String[]{TestFixtures.USER3_EMAIL, TestFixtures.USER1_EMAIL, });
		delegations.add(new String[]{TestFixtures.USER4_EMAIL, TestFixtures.USER1_EMAIL, });
		delegations.add(new String[]{TestFixtures.USER5_EMAIL, TestFixtures.USER2_EMAIL, });  // 5 -> 2 requested, because user2 is NOT a public proxy
		delegations.add(new String[]{TestFixtures.USER6_EMAIL, TestFixtures.USER3_EMAIL, });
		delegations.add(new String[]{TestFixtures.USER7_EMAIL, TestFixtures.USER4_EMAIL, });
		delegations.add(new String[]{TestFixtures.USER10_EMAIL, TestFixtures.USER7_EMAIL, });
		delegations.add(new String[]{TestFixtures.USER11_EMAIL, TestFixtures.USER7_EMAIL, });
		delegations.add(new String[]{TestFixtures.USER12_EMAIL, TestFixtures.USER7_EMAIL, });
	}


  //TODO: REST URLs should also be part of these TestFixtures. When an URL changes in the backend this SHOULD break a test.
}