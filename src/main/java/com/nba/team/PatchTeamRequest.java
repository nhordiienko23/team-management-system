package com.nba.team;

import jakarta.validation.constraints.Min;

public record PatchTeamRequest (String teamName,
                                @Min(value = 0, message = "Championships Won cannot be negative")
                                Integer championshipCount){
}
