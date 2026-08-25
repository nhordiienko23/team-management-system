package com.nba.team;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ResponseTeamDto(
        Long id,
        String name,
        Integer championshipTitleCount,
        LocalDate creationDate,
        List<String> teamMembers
) {
}
