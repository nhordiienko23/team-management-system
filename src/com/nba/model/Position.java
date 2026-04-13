package com.nba.model;

public enum Position {
    PG("Point Guard"),
    SG("Shooting Guard"),
    SF("Small Forward"),
    PF("Power Forward"),
    C("Center");

    private final String fullname;

    Position(String fullname) {
        this.fullname = fullname;
    }

    public String getFullname() {
        return fullname;
    }
}
