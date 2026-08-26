package com.nba.player;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.mapper.MemberShortMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
class PlayerMapperImpl implements PlayerMapper {

    private final MemberShortMapper memberShortMapper;

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
    public TeamGroupResponse toResponseTeammates(List<Player> players, Player player) {
        if (players == null || players.isEmpty()) return TeamGroupResponse.builder()
                .members(Collections.emptyList())
                .build();

        String teamName = players.get(0).getTeam().getName();
        return TeamGroupResponse.builder()
                .teamName(teamName)
                .title("TEAMMATES OF " + player.getFirstName() + " " + player.getLastName())
                .members(players.stream()
                        .filter(p -> !p.getId().equals(player.getId()))
                        .map(memberShortMapper::toMemberShortDto)
                        .toList())
                .build();
    }
}