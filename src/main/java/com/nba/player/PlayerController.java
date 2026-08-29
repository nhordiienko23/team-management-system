package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;


    @Operation(summary = "returns list of all players")
    @GetMapping
    public ResponseEntity<Page<ResponsePlayerDto>> getAllPlayers(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(playerService.getAllPlayers(pageable));
    }

    @Operation(summary = "returns player by id")
    @GetMapping("/{id}")
    public ResponseEntity<ResponsePlayerDto> getPlayerById(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayerById(id));
    }

    @Operation(summary = "returns list of players flexibly filtered by any combination of parameters")
    @GetMapping("/search")
    public ResponseEntity<Page<ResponsePlayerDto>> searchPlayers(@ParameterObject PlayerSearchFilter filter,
                                                                 @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(playerService.searchPlayers(filter,pageable));
    }

    @Operation(summary = "returns list of teammates")
    @GetMapping("/{playerId}/teammates")
    public ResponseEntity<TeamGroupResponse> getTeammates(@PathVariable Long playerId) {
        return ResponseEntity.ok(playerService.getTeammatesByPlayerId(playerId));
    }


}