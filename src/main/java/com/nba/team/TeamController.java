package com.nba.team;

import com.nba.core.dto.response.TeamGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@AllArgsConstructor
@Tag(name = "team API", description = "Endpoints for managing team information")
public class TeamController {
    private final TeamService teamService;

    @Operation(summary = "returns list of all teams")
    @GetMapping
    public ResponseEntity<List<ResponseTeamDto>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @Operation(summary = "returns team by id")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseTeamDto> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }


    @Operation(summary = "Returns list of teams flexibly filtered by any combination of parameters")
    @GetMapping("/search")
    public ResponseEntity<List<ResponseTeamDto>> searchTeam(@ParameterObject TeamSearchFilter filter) {
        return ResponseEntity.ok(teamService.searchTeams(filter));
    }


    @Operation(summary = "Returns team lineup")
    @GetMapping("/{teamId}/team-lineup")
    public ResponseEntity<TeamGroupResponse> getTeamLineup(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamLineup(teamId));
    }

    @Operation(summary = "Returns coaching staff")
    @GetMapping("/{teamId}/coaching-staff")
    public ResponseEntity<TeamGroupResponse> getCoachingStaff(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getCoachingStaff(teamId));
    }
}