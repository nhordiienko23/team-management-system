package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import java.util.List;

interface PlayerMapper {
    Player toPlayerEntity(RequestPlayerDto dto);
    ResponsePlayerDto toPlayerDto(Player player);
    TeamGroupResponse toResponseTeammates(List<Player> players, Player player);
}