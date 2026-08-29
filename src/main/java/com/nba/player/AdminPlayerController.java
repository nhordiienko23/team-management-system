package com.nba.player;

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
@RequestMapping("/api/admin/players")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "player API", description = "Endpoints for managing player information")
public class AdminPlayerController {
    private final PlayerService playerService;

    @Operation(summary = "creates new player")
    @PostMapping
    public ResponseEntity<ResponsePlayerDto> createPlayer(
            @Valid @RequestBody RequestPlayerDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(playerService.addPlayer(dto));
    }

    @Operation(summary = "partially updates player by id")
    @PatchMapping("/{id}")
    public ResponseEntity<ResponsePlayerDto> partialUpdatePlayer(
            @PathVariable Long id,
            @Valid @RequestBody PatchPlayerRequest request) {
        return ResponseEntity.ok(playerService.partialUpdatePlayer(id, request));
    }

    @Operation(summary = "deletes player by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Player with id " + id + " was deleted successfully")
                .build());
    }

    @Operation(summary = "changes player team or makes him a free agent if teamId is not provided")
    @PatchMapping("/{playerId}/change-team")
    public ResponseEntity<TeamTransferResponse> changePlayerTeam(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long newTeamId) {
        return ResponseEntity.ok(playerService.changePlayerTeam(playerId, newTeamId));
    }
}
