package com.nba.team;

import com.nba.core.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@AllArgsConstructor
@Tag(name = "team API")
public class TeamController {
    private final TeamService teamService;

    @Operation(summary = "creates new team")
    @PostMapping
    public ResponseEntity<ResponseTeamDto> createTeam(@Valid @RequestBody RequestTeamDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.addTeam(dto));
    }

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

    @Operation(summary = "deletes team by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Team with id " + id + " was deleted successfully")
                .build());
    }
    @Operation(summary = "adds player to team by id")
    @PostMapping("/{teamId}/players/{playerId}")
    public ResponseEntity<MessageResponse> addPlayer(@PathVariable Long teamId,
                                                     @PathVariable Long playerId) {
        teamService.addPlayerToTeam(teamId, playerId);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Player with id " + playerId + " was added " +
                        "to team with id " + teamId + " successfully")
                .build());
    }
    @Operation(summary = "adds coach to team by id")
    @PostMapping("/{teamId}/coaches/{coachId}")
    public ResponseEntity<MessageResponse> addCoach(@PathVariable Long teamId,
                                                    @PathVariable Long coachId) {
        teamService.addCoachToTeam(teamId, coachId);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Coach with id " + coachId + " was added " +
                        "to team with id " + teamId + " successfully")
                .build());
    }
    @Operation(summary = "deletes player from team by id")
    @DeleteMapping("/{teamId}/players/{playerId}")
    public ResponseEntity<MessageResponse> removePlayer(@PathVariable Long teamId,
                                                        @PathVariable Long playerId) {
        teamService.deletePlayerFromTeam(teamId, playerId);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Player with id " + playerId + " was removed " +
                        "from team with id " + teamId + " successfully")
                .build());
    }
    @Operation(summary = "deletes coach from team by id")
    @DeleteMapping("/{teamId}/coaches/{coachId}")
    public ResponseEntity<MessageResponse> removeCoach(@PathVariable Long teamId,
                                                       @PathVariable Long coachId) {
        teamService.deleteCoachFromTeam(teamId, coachId);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Coach with id " + coachId + " was removed " +
                        "from  team with id " + teamId + " successfully")
                .build());
    }

    @Operation(summary = "deletes all team members from team by team id")
    @PostMapping("/fire-team/{id}")
    public ResponseEntity<MessageResponse> fireAllTeamMembers(@PathVariable Long id) {
        teamService.fireAllTeamMembers(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("all team members were fired from team with id " + id)
                .build());
    }
    @Operation(summary = "updates team by id")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseTeamDto> updateTeam(@PathVariable Long id,
                                                      @Valid @RequestBody RequestTeamDto dto) {
        return ResponseEntity.ok(teamService.updateTeam(id, dto));
    }
    @Operation(summary = "Returns list of teams flexibly filtered by any combination of parameters")
    @GetMapping("/search")
    public ResponseEntity<List<ResponseTeamDto>> searchTeam(@ParameterObject TeamSearchFilter filter) {
        return ResponseEntity.ok(teamService.searchTeams(filter));
    }

    @GetMapping("/{teamId}/team-lineup")
    public ResponseEntity<ResponseGroupType> getTeamLineup(@PathVariable Long teamId){
        return ResponseEntity.ok(teamService.getTeamLineup(teamId));
    }
    @GetMapping("/{teamId}/coaching-staff")
    public ResponseEntity<ResponseGroupType> getCoachingStaff(@PathVariable Long teamId){
        return ResponseEntity.ok(teamService.getCoachingStaff(teamId));
    }


}
