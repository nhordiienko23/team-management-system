package com.nba.team;

import com.nba.core.dto.response.MemberShortDto;
import lombok.Builder;

import java.util.List;

@Builder
public record ResponseTeamDto(
        Long id,
        String name,
        Integer championshipTitleCount,
        Integer creationYear,
        List<MemberShortDto> coaches,
        List<MemberShortDto> players
) {
}
