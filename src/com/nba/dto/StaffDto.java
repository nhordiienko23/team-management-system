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
    @Min(0)
    @Max(100)
    public int rating;

    public List<Position> positions;

    // fields for coach
    @Min(0)
    public int experienceYears;

    @Min(0)
    public int championshipsWon;

    @Override
    public String toString() {
        java.util.List<String> fields = new java.util.ArrayList<>();
        if (type != null && !type.isEmpty()) fields.add("type='" + type + "'");
        if (name != null && !name.isEmpty()) fields.add("name='" + name + "'");
        if (baseSalary != 0) fields.add("baseSalary=" + baseSalary);
        if (rating != 0) fields.add("rating=" + rating);
        if (positions != null && !positions.isEmpty()) fields.add("positions=" + positions);
        if (experienceYears != 0) fields.add("experienceYears=" + experienceYears);
        if (championshipsWon != 0) fields.add("championshipsWon=" + championshipsWon);
        return "StaffDto{" + String.join(", ", fields) + "}";
    }
}