package com.nba.team;

import com.nba.core.dto.response.TeamGroupResponse;
import java.util.List;

public interface TeamService {
    ResponseTeamDto addTeam(RequestTeamDto dto);
    List<ResponseTeamDto> getAllTeams();
    ResponseTeamDto updateTeam(Long teamId, RequestTeamDto dto);
    void deleteTeam (Long teamId);
    ResponseTeamDto getTeamById(Long teamId);
    List<ResponseTeamDto> searchTeams(TeamSearchFilter filter);
    void addPlayerToTeam(Long teamId,Long playerId);
    void addCoachToTeam(Long teamId, Long coachId);
    void deletePlayerFromTeam(Long teamId, Long playerId);
    void deleteCoachFromTeam(Long teamId, Long coachId);
    void fireAllTeamMembers(Long teamId);
    TeamGroupResponse getTeamLineup(Long teamId);
    TeamGroupResponse getCoachingStaff(Long teamId);

}