package com.nba.coach;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PatchCoachRequest(

        String firstName,

        String lastName,

        @Positive(message = "Salary must be greater than zero")
        BigDecimal salary,

        Long teamId,

        @Min(value = 0, message = "Experience cannot be negative")
        Integer yearsOfExperience,

        @Min(value = 0, message = "Championships Won cannot be negative")
        Integer championshipsWon
) {

}
