package com.nba.player;

import java.math.BigDecimal;
import java.util.List;

 record PlayerSearchFilter(
        String firstName,
        String lastName,
        String teamName,
        Integer minRating,
        Integer maxRating,
        Integer minChampionshipWon,
        Integer maxChampionshipWon,
        List<String> positions,
        BigDecimal minSalary,
        BigDecimal maxSalary
) {
}