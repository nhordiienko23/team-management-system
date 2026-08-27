package com.nba.player;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record PatchPlayerRequest(

        String firstName,

        String lastName,

        @Positive(message = "Salary must be greater than zero")
        BigDecimal salary,

        Long teamId,

        Set<PlayerPosition> playerPositions,

        @Min(value = 0, message = "Rating cannot be less than 0")
        @Max(value = 100, message = "Rating cannot be more than 100")

        Integer rating,
        @Min(value = 0, message = "Championships Won cannot be negative")
        Integer championshipsWon) {
}
