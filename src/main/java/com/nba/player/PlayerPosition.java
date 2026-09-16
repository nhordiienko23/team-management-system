package com.nba.player;

import java.util.List;

 public enum PlayerPosition {
    PG("Point Guard"),
    SG("Shooting Guard"),
    SF("Small Forward"),
    PF("Power Forward"),
    C("Center");

    private final String fullname;

    PlayerPosition(String fullname) {
        this.fullname = fullname;
    }

    public String getFullname() {
        return fullname;
    }

    public static List<PlayerPosition> findByPartialName(String searchText) {
        if (searchText == null || searchText.isBlank()) return null;
        String lowerSearch = searchText.toLowerCase();

        return java.util.Arrays.stream(values())
                .filter(pos -> pos.name().toLowerCase().contains(lowerSearch) ||
                        pos.getFullname().contains(lowerSearch))
                .toList();
    }
}
