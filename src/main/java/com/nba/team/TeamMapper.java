package com.nba.team;

import com.nba.core.dto.response.TeamGroupResponse;

interface TeamMapper {
    Team toTeamEntity(RequestTeamDto dto);
    ResponseTeamDto toTeamDto(Team team);
    TeamGroupResponse toPlayerLineup(Team team);
    TeamGroupResponse toCoachingStaff(Team team);
}