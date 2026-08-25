package com.nba.core.exception.notFound;

public class PlayerNotFoundException extends ResourceNotFoundException {
    public PlayerNotFoundException(Long id) {
        super("Player with id "+id+" not found");
    }
}
