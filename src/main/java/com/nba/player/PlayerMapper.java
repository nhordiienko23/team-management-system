package com.nba.player;

import com.nba.core.dto.response.MemberShortDto;
import com.nba.core.model.TeamMember;

import java.util.List;

 interface PlayerMapper {
    Player toPlayerEntity(RequestPlayerDto dto);
    ResponsePlayerDto toPlayerDto(Player player);
    ResponseTeammates toResponseTeammates(List<Player> players, Player player);
    MemberShortDto toMemberShortDto(Player player);
}
