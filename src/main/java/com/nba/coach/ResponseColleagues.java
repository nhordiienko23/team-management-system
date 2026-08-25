package com.nba.coach;

import com.nba.core.dto.response.MemberShortDto;
import lombok.Builder;

import java.util.List;

@Builder
public record ResponseColleagues(
        String teamName,
        String coachFullName,
        List<MemberShortDto> colleagues
) {
}
