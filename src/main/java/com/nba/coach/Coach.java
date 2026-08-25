package com.nba.coach;

import com.nba.core.model.TeamMember;
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
}