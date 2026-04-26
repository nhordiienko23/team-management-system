package com.nba.dto;

import com.nba.model.Position;
import jakarta.validation.constraints.*;
import java.util.List;

public class StaffDto {

    @NotBlank(message = "Type is mandatory")
    public String type;

    @NotBlank(message = "Name is mandatory")
    public String name;

    @Positive(message = "Base salary must be greater than zero")
    public double baseSalary;

    // fields for player
    @Min(0) @Max(100)
    public int rating;

    public List<Position> positions;

    // fields for coach
    @Min(0)
    public int experienceYears;

    @Min(0)
    public int championshipsWon;
}