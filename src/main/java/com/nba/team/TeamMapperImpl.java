package com.nba.team;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

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
                .championshipTitleCount(team.getChampionshipTitleCount())
                .teamMembers(
                        Optional.ofNullable(team.getTeamMembers())
                                .orElse(Collections.emptyList())
                                .stream()
                                .map(teamMember -> teamMember.getClass().getSimpleName() + ": id " + teamMember.getId() + " " + teamMember.getFirstName() + " " + teamMember.getLastName())
                                .toList()
                )
                .build();
    }
}

