package com.nba.team;

import java.util.List;

public interface TeamService {
    ResponseTeamDto addTeam(RequestTeamDto dto);
    List<ResponseTeamDto> getAllTeams();
    ResponseTeamDto updateTeam(Long id, RequestTeamDto dto);
    void deleteTeam (Long id);
    ResponseTeamDto getTeamById(Long id);
    List<ResponseTeamDto> searchTeams(TeamSearchFilter filter);
    void addPlayerToTeam(Long teamId,Long playerId);
    void addCoachToTeam(Long teamId, Long coachId);
    void deletePlayerFromTeam(Long teamId, Long playerId);
    void deleteCoachFromTeam(Long teamId, Long coachId);
    void fireAllTeamMembers(Long teamId);
}
