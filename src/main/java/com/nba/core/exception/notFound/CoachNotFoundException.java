package com.nba.core.exception.notFound;

public class CoachNotFoundException extends ResourceNotFoundException {
    public CoachNotFoundException(Long id) {
        super("Coach with id " + id + " not found");
    }
}
