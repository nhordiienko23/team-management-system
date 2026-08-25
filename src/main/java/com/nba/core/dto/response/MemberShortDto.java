package com.nba.core.dto.response;

import lombok.Builder;

@Builder
public record MemberShortDto(
        Long id,
        String fullName
) {
}
