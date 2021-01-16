package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.TeamModel;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

/**
 * Interface to database for Teams.
 * with sorting, paging and advanced filtering support.
 */
public interface TeamRepo extends PagingAndSortingRepository<TeamModel, Long> {
	//TODO: Advancd filter capability for Teams:   , JpaSpecificationExecutor<TeamModel>

	Optional<TeamModel> findByInviteCode(String inviteCode);

	Optional<TeamModel> findByTeamName(String teamName);

	// Did i already mention that Spring-data-jpa's query creation from interface method names is absolutely evil dark wizardry :-)
	Optional<TeamModel> findByMembersEmailEquals(String email);
}
