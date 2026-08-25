package com.nba.player;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ResponsePlayerDto(
        Long id,
        String firstName,
        String lastName,
        String teamRole,
        List positions,
        String team,
        Integer rating,
        Integer championshipWon,
        BigDecimal salary
) {
}
