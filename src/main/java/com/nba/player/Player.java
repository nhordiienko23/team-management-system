package com.nba.player;

import com.nba.core.exception.invalidData.InvalidTeamDataException;
import com.nba.core.model.TeamMember;
import com.nba.team.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Set;

@Entity
@DiscriminatorValue("PLAYER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Player extends TeamMember {

    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "player_positions", joinColumns = @JoinColumn(name = "player_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private Set<PlayerPosition> playerPositions;

    private Integer rating;


    @Override
    public void checkTeamLimits(Team team) {
        if (team.getPlayers().size() >= 15) {
            throw new InvalidTeamDataException("The team roster is full. Maximum 15 players allowed.");
        }
    }
}