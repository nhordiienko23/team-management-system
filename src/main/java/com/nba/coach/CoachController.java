package com.nba.coach;

import com.nba.core.dto.response.MessageResponse;
import com.nba.core.dto.response.TeamGroupResponse;
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
@RequestMapping("/api/coaches")
@AllArgsConstructor
@Tag(name = "coach API",description = "Endpoints for managing coach information")
public class CoachController {
    private final CoachService coachService;

    @Operation(summary = "creates new coach")
    @PostMapping
    public ResponseEntity<ResponseCoachDto> addCoach(@Valid @RequestBody RequestCoachDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coachService.addCoach(dto));
    }

    @Operation(summary = "returns coach by id")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseCoachDto> getCoachById(@PathVariable Long id) {
        return ResponseEntity.ok(coachService.getCoachById(id));
    }

    @Operation(summary = "returns list of all coaches")
    @GetMapping
    public List<ResponseCoachDto> getAllCoaches() {
        return coachService.getAllCoaches();
    }

    @Operation(summary = "Returns list of coaches flexibly filtered by any combination of parameters")
    @GetMapping("/search")
    public List<ResponseCoachDto> searchCoaches(
            @ParameterObject CoachSearchFilter filter) {
        return coachService.searchCoaches(filter);
    }

    @Operation(summary = "partially updates coach by id")
    @PatchMapping("/{id}")
    public ResponseEntity<ResponseCoachDto> updateCoach(@PathVariable Long id,
                                                        @Valid @RequestBody PatchCoachRequest request) {
        return ResponseEntity.ok(coachService.partialUpdateCoach(id, request));
    }

    @Operation(summary = "deletes coach by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCoach(@PathVariable Long id) {
        coachService.deleteCoach(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Coach with id " + id + " was deleted successfully")
                .build());
    }

    @Operation(summary = "returns list of colleagues")
    @GetMapping("/{coachId}/colleagues")
    public ResponseEntity<TeamGroupResponse> getColleagues(@PathVariable Long coachId) {
        return ResponseEntity.ok(coachService.getColleaguesByCoachId(coachId));
    }
    @Operation(summary = "changes coach team or makes him a free agent if teamId is not provided")
    @PatchMapping("/{coachId}/change-team")
    public ResponseEntity<MessageResponse> changeCoachTeam(@PathVariable Long coachId,
                                                           @RequestParam(required = false) Long newTeamId) {
        coachService.changeCoachTeam(coachId, newTeamId);
        return ResponseEntity.ok(MessageResponse.builder()
                .message(newTeamId == null
                        ? "Coach with id " + coachId + " became a free agent successfully"
                        : "Coach with id " + coachId +
                          " joined team with id " + newTeamId + " successfully")
                .build());
    }

}