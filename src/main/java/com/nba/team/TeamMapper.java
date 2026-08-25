package com.nba.team;

import com.nba.core.dto.response.MemberShortDto;
import com.nba.core.model.TeamMember;



interface TeamMapper {
    Team toTeamEntity(RequestTeamDto dto);
    ResponseTeamDto toTeamDto(Team team);
    MemberShortDto toMemberShortDto(TeamMember teamMember);
    ResponseGroupType toPlayerLineup (Team team);
    ResponseGroupType toCoachingStaff (Team team);
}
