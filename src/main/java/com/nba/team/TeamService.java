package com.nba.team;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeamService {
    ResponseTeamDto addTeam(RequestTeamDto dto);
    Page<ResponseTeamDto> getAllTeams(Pageable pageable);
    ResponseTeamDto partialUpdateTeam(Long teamId, PatchTeamRequest request);
    void deleteTeam (Long teamId);
    ResponseTeamDto getTeamById(Long teamId);
    Page<ResponseTeamDto> searchTeams(TeamSearchFilter filter, Pageable pageable);
    TeamTransferResponse addPlayerToTeam(Long teamId, Long playerId);
    TeamTransferResponse addCoachToTeam(Long teamId, Long coachId);
    TeamTransferResponse deletePlayerFromTeam(Long teamId, Long playerId);
    TeamTransferResponse deleteCoachFromTeam(Long teamId, Long coachId);
    void fireAllTeamMembers(Long teamId);
    TeamGroupResponse getTeamLineup(Long teamId);
    TeamGroupResponse getCoachingStaff(Long teamId);

}