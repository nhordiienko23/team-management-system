package com.nba.core.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record TeamGroupResponse(
        String teamName,
        String title,
        List<MemberShortDto> members
) {
}