package com.nba.player;

import java.util.List;

 interface PlayerMapper {
    Player toPlayerEntity(RequestPlayerDto dto);
    ResponsePlayerDto toPlayerDto(Player player);
    ResponseTeammates toResponseTeammates(List<Player> players, Player player);
}
