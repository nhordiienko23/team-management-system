package com.nba.player;

import com.nba.core.dto.response.MessageResponse;
import com.nba.core.dto.response.TeamGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@AllArgsConstructor
@Tag(name = "player API", description = "Endpoints for managing player information")
public class PlayerController {

    private final PlayerService playerService;

    @Operation(summary = "creates new player")
    @PostMapping
    public ResponseEntity<ResponsePlayerDto> createPlayer(
            @Valid @RequestBody RequestPlayerDto dto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(playerService.addPlayer(dto));
    }

    @Operation(summary = "returns list of all players")
    @GetMapping
    public ResponseEntity<List<ResponsePlayerDto>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }

    @Operation(summary = "updates player by id")
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

    @Operation(summary = "returns player by id")
    @GetMapping("/{id}")
    public ResponseEntity<ResponsePlayerDto> getPlayerById(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayerById(id));
    }

    @Operation(summary = "returns list of players flexibly filtered by any combination of parameters")
    @GetMapping("/search")
    public ResponseEntity<List<ResponsePlayerDto>> searchPlayers(@ParameterObject PlayerSearchFilter filter) {
        return ResponseEntity.ok(playerService.searchPlayers(filter));
    }

    @Operation(summary = "returns list of teammates")
    @GetMapping("/{playerId}/teammates")
    public ResponseEntity<TeamGroupResponse> getTeammates(@PathVariable Long playerId) {
        return ResponseEntity.ok(playerService.getTeammatesByPlayerId(playerId));
    }

    @Operation(summary = "trades player to another team or makes them a free agent if teamId is not provided")
    @PatchMapping("/{playerId}/change-team")
    public ResponseEntity<MessageResponse> changePlayerTeam(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long teamId) {

        playerService.changePlayerTeam(playerId, teamId);

        String message;
        if (teamId == null) {
            message = "Player with id " + playerId + " became a free agent successfully";
        } else {
            message = "Player with id " + playerId + " was traded to team with id " + teamId + " successfully"; // <--- Добавили пробел перед was
        }

        return ResponseEntity.ok(MessageResponse.builder()
                .message(message)
                .build());
    }

}