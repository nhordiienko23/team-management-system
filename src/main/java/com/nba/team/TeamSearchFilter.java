package com.nba.team;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record TeamSearchFilter(
        String teamName,
        Integer minChampionshipTitleCount,
        Integer maxChampionshipTitleCount,

        // Добавляем красивый формат
        @DateTimeFormat(pattern = "dd.MM.yyyy")
        LocalDate creationDateStart,

        @DateTimeFormat(pattern = "dd.MM.yyyy")
        LocalDate creationDateEnd
) {
}