package com.nba.team;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record RequestTeamDto(
        @NotBlank(message = "Name cannot be blank")
        String name,

        @NotNull(message = "Championships count is required")
        @Min(value = 0, message = "Championships Won cannot be negative")
        Integer championshipCount,

        @NotNull(message = "Creation year is required")
        @Min(value = 0, message = "Creation Year cannot be negative")
        Integer creationYear
) {
}