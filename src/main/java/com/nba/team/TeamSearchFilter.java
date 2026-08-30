package com.nba.team;

public record TeamSearchFilter(
        String teamName,
        Integer minChampionshipTitleCount,
        Integer maxChampionshipTitleCount,
        Integer minCreationYear,
        Integer maxCreationYear
) {
}