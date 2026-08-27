package com.nba.player;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record RequestPlayerDto(
        @NotBlank(message = "First teamName cannot be blank")
        String firstName,

        @NotBlank(message = "Last teamName cannot be blank")
        String lastName,

        @NotNull(message = "Salary cannot be null")
        @Positive(message = "Salary must be greater than zero")
        BigDecimal salary,

        Long teamId,

        @NotEmpty(message = "Player must have at least one position")
        Set<PlayerPosition> playerPositions,

        @NotNull(message = "Rating is required")
        @Min(value = 0, message = "Rating cannot be less than 0")
        @Max(value = 100, message = "Rating cannot be more than 100")
        Integer rating,
        @Min(value = 0, message = "Championships Won cannot be negative")
        Integer championshipsWon

) {
}
