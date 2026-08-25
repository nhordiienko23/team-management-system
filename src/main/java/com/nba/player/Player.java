package com.nba.player;

import com.nba.core.model.TeamMember;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

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
    private Set<PlayerPosition> playerPositions;

    private Integer rating;
}