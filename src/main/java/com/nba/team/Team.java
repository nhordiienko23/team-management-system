package com.nba.team;


import com.nba.coach.Coach;
import com.nba.core.model.TeamMember;
import com.nba.player.Player;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String name;
    @Column(nullable = false)
    private LocalDate creationDate;
    @Column(nullable = false)
    private Integer championshipTitleCount;

    @Builder.Default
    @OneToMany(mappedBy = "team", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<TeamMember> teamMembers = new ArrayList<>();

    public List<Player> getPlayers() {
        return teamMembers.stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .toList();
    }

    public List<Coach> getCoaches() {
        return teamMembers.stream()
                .filter(Coach.class::isInstance)
                .map(Coach.class::cast)
                .toList();
    }

    public void addTeamMember(TeamMember teamMember) {
        this.getTeamMembers().add(teamMember);
        teamMember.setTeam(this);
    }
    public void removeTeamMember(TeamMember teamMember){
        this.getTeamMembers().remove(teamMember);
        teamMember.setTeam(null);
    }
}
