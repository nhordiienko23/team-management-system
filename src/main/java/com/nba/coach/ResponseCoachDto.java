package com.nba.coach;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ResponseCoachDto (
        Long id,
        String firstName,
        String lastName,
        String teamRole,
        String teamName,
        Integer yearsExperience,
        Integer championshipWon,
        BigDecimal salary
){
}
