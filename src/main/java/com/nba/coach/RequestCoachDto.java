package com.nba.coach;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RequestCoachDto(
        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        @NotNull(message = "Salary cannot be null")
        @Positive(message = "Salary must be greater than zero")
        BigDecimal salary,

        Long teamId,

        @NotNull(message = "Experience is required")
        @Min(value = 0, message = "Experience cannot be negative")
        Integer yearsOfExperience,

        @NotNull(message = "Championships Won is required")
        @Min(value = 0, message = "Championships Won cannot be negative")
        Integer championshipsWon
) {
}