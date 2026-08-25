package com.nba.player;

import lombok.Builder;

import java.util.List;

@Builder
public record ResponseTeammates(
        String teamName,
        String playerFullName,
        List<String> teammates) {
}
