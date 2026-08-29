package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlayerService {
    ResponsePlayerDto addPlayer(RequestPlayerDto dto);
    Page<ResponsePlayerDto> getAllPlayers(Pageable pageable);
    ResponsePlayerDto partialUpdatePlayer(Long playerId, PatchPlayerRequest request);
    void deletePlayer(Long playerId);
    ResponsePlayerDto getPlayerById(Long playerId);
    Page<ResponsePlayerDto> searchPlayers(PlayerSearchFilter filter,Pageable pageable);
    TeamGroupResponse getTeammatesByPlayerId(Long playerId);
    TeamTransferResponse changePlayerTeam(Long playerId, Long newTeamId);
}