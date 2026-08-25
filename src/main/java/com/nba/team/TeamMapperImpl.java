package com.nba.team;

import com.nba.core.dto.response.MemberShortDto;
import com.nba.core.model.TeamMember;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;

@Component
class TeamMapperImpl implements TeamMapper {
    @Override
    public Team toTeamEntity(RequestTeamDto dto) {
        if (dto == null) {
            return null;
        }

        return Team.builder()
                .name(dto.name())
                .championshipTitleCount(dto.championshipCount())
                .creationDate(LocalDate.now())
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
                .creationDate(team.getCreationDate())
                .coaches(team.getCoaches().stream()
                        .map(this::toMemberShortDto)
                        .toList())
                .players(team.getPlayers().stream()
                        .map(this::toMemberShortDto)
                        .toList())
                .championshipTitleCount(team.getChampionshipTitleCount())
                .build();
    }

    @Override
    public MemberShortDto toMemberShortDto(TeamMember teamMember) {
        return MemberShortDto.builder()
                .id(teamMember.getId())
                .fullName(teamMember.getFirstName() + " " + teamMember.getLastName())
                .build();
    }

    @Override
    public ResponseGroupType toPlayerLineup(Team team) {
        return ResponseGroupType.builder()
                .teamName(team.getName())
                .groupType(TeamGroupType.TEAM_LINEUP)
                .members(team.getPlayers().stream().
                        map(this::toMemberShortDto)
                        .toList())
                .build();
    }

    @Override
    public ResponseGroupType toCoachingStaff(Team team) {
        return ResponseGroupType.builder()
                .teamName(team.getName())
                .groupType(TeamGroupType.COACHING_STAFF)
                .members(team.getCoaches().stream().
                        map(this::toMemberShortDto)
                        .toList())
                .build();
    }


}

