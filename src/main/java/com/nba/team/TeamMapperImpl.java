package com.nba.team;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.mapper.MemberShortMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
class TeamMapperImpl implements TeamMapper {

    private final MemberShortMapper memberShortMapper;

    @Override
    public Team toTeamEntity(RequestTeamDto dto) {
        if (dto == null) {
            return null;
        }

        return Team.builder()
                .name(dto.name())
                .championshipTitleCount(dto.championshipCount())
                .creationYear(dto.creationYear())
                .teamMembers(new ArrayList<>())
                .build();
    }

    @Override
    public ResponseTeamDto toTeamDto(Team team) {
        if (team == null) {
            return null;
        }

        return ResponseTeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .creationYear(team.getCreationYear())
                .coaches(team.getCoaches().stream()
                        .map(memberShortMapper::toMemberShortDto)
                        .toList())
                .players(team.getPlayers().stream()
                        .map(memberShortMapper::toMemberShortDto)
                        .toList())
                .championshipTitleCount(team.getChampionshipTitleCount())
                .build();
    }

    @Override
    public TeamGroupResponse toPlayerLineup(Team team) {
        return TeamGroupResponse.builder()
                .teamName(team.getName())
                .title(TeamGroupType.TEAM_LINEUP.name())
                .members(team.getPlayers().stream()
                        .map(memberShortMapper::toMemberShortDto)
                        .toList())
                .build();
    }

    @Override
    public TeamGroupResponse toCoachingStaff(Team team) {
        return TeamGroupResponse.builder()
                .teamName(team.getName())
                .title(TeamGroupType.COACHING_STAFF.name())
                .members(team.getCoaches().stream()
                        .map(memberShortMapper::toMemberShortDto)
                        .toList())
                .build();
    }
}