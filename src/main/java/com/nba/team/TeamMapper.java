package com.nba.team;

 interface TeamMapper {
    Team toTeamEntity(RequestTeamDto dto);
    ResponseTeamDto toTeamDto(Team team);
}
