package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

/**
 * Interface to database for Teams.
 * with sorting, paging and advanced filtering support.
 */
public interface TeamRepo extends PagingAndSortingRepository<TeamModel, Long> {
	//TODO: Advanced filter capability for Teams:   , JpaSpecificationExecutor<TeamModel>

	Optional<TeamModel> findByInviteCode(String inviteCode);

	Optional<TeamModel> findByTeamName(String teamName);

	Optional<TeamModel> findByIdAndAdminsIdEquals(Long teamId, Long adminId);

	// Did i already mention that Spring-data-jpa's query creation from interface method names is absolutely evil dark wizardry :-)
	Optional<TeamModel> findByMembersEmailEquals(String email);

	@Query("FROM TeamModel t WHERE :user member t.members OR :user member t.admins")
	List<TeamModel> teamsOfUser(UserModel user);

	/*
	 * @Deprecated.  See methods directly in TeamModel
	 * Check if a given user is member <b>or</b> admin of a team
	 * @param teamId a team
	 * @param memberId member or admin
	 * @return true if memberId is member or admin of this team

	@Query("SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM TEAMS_ADMINS admins, TEAMS_MEMBER members WHERE " +
		     "(admins.TEAM_MODEL_ID = :teamId AND admins.ADMIN_ID = :memberId ) OR (members.TEAM_MODEL_ID = :teamId AND members.MEMBERS_ID = :memberId)")
	boolean isMemberOrAdminOfTeam(Long teamId, Long memberId);
	*/
}
