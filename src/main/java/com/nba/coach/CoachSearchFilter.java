package com.nba.coach;

import java.math.BigDecimal;

public record CoachSearchFilter(
        String firstName,
        String lastName,
        String teamName,
        Integer minChampionshipsWon,
        Integer maxChampionshipsWon,
        Integer minYearsOfExperience,
        Integer maxYearsOfExperience,
        BigDecimal minSalary,
        BigDecimal maxSalary
) {
}
