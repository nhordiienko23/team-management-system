package com.nba.core.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamTransferResponse(
        String message,
        Long memberId,
        String memberFullName,
        String memberRole,
        String oldTeamName,
        Long newTeamId,
        String newTeamName
) {
}