package com.nba.coach;

import com.nba.core.exception.invalidData.InvalidTeamDataException;
import com.nba.core.model.TeamMember;
import com.nba.team.Team;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("COACH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
 public class Coach extends TeamMember {
    private Integer yearsOfExperience;

    @Override
    public void checkTeamLimits(Team team) {
        if (team.getCoaches().size() >= 5) {
            throw new InvalidTeamDataException("The coaching staff is full. Maximum 5 coaches allowed.");
        }
    }
}