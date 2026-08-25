package com.nba.team;

import com.nba.core.dto.response.MemberShortDto;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ResponseTeamDto(
        Long id,
        String name,
        Integer championshipTitleCount,
        LocalDate creationDate,
        List<MemberShortDto> coaches,
        List<MemberShortDto> players
) {
}
