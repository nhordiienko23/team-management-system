package com.nba.player;

import com.nba.core.dto.response.MemberShortDto;
import com.nba.core.model.TeamMember;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
 class PlayerMapperImpl implements PlayerMapper {
    @Override
    public Player toPlayerEntity(RequestPlayerDto dto) {
        if (dto == null) return null;

        return Player.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .salary(dto.salary())
                .playerPositions(dto.playerPositions())
                .rating(dto.rating())
                .championshipsWon(dto.championshipsWon())
                .build();
    }

    @Override
    public ResponsePlayerDto toPlayerDto(Player player) {
        if (player == null) return null;

        return ResponsePlayerDto.builder()
                .id(player.getId())
                .firstName(player.getFirstName())
                .lastName(player.getLastName())
                .teamRole("Player")
                .salary(player.getSalary())
                .positions(player.getPlayerPositions().stream()
                        .map(playerPosition -> playerPosition.name())
                        .toList())
                .team((player.getTeam() != null) ? player.getTeam().getName() : null)
                .rating(player.getRating())
                .championshipWon(player.getChampionshipsWon())
                .build();
    }

    @Override
    public ResponseTeammates toResponseTeammates(List<Player> players,Player player) {
        if (players == null || players.isEmpty()) return ResponseTeammates.
                builder()
                .teammates(Collections.emptyList())
                .build();
        String teamName = players.get(0).getTeam().getName();
        return ResponseTeammates.builder()
                .teamName(teamName)
                .playerFullName(player.getFirstName()+" "+player.getLastName())
                .teammates(players.stream()
                        .filter(p-> !p.getId().equals(player.getId()))
                        .map(this::toMemberShortDto)
                        .toList())
                .build();
    }
    @Override
    public MemberShortDto toMemberShortDto(Player player) {
        return MemberShortDto.builder()
                .id(player.getId())
                .fullName(player.getFirstName() + " " + player.getLastName())
                .build();
    }

}
