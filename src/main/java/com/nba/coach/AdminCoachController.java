package com.nba.coach;

import com.nba.core.dto.response.MessageResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/coaches")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "coach API", description = "Endpoints for managing coach information")
public class AdminCoachController {

    private final CoachService coachService;

    @Operation(summary = "creates new coach")
    @PostMapping
    public ResponseEntity<ResponseCoachDto> addCoach(@Valid @RequestBody RequestCoachDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coachService.addCoach(dto));
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

    @Operation(summary = "changes coach team or makes him a free agent if teamId is not provided")
    @PatchMapping("/{coachId}/change-team")
    public ResponseEntity<TeamTransferResponse> changeCoachTeam(@PathVariable Long coachId,
                                                                @RequestParam(required = false) Long newTeamId) {

        return ResponseEntity.ok(coachService.changeCoachTeam(coachId, newTeamId));
    }
}
