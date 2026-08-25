package com.nba.core.exception.notFound;

public class TeamNotFoundException extends ResourceNotFoundException {
    public TeamNotFoundException(Long id) {
        super("Team with id " + id + " not found");
    }
}
