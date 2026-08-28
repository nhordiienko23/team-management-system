package com.nba.core.exception.notFound;

public class UserNotFoundException extends ResourceNotFoundException {
    public UserNotFoundException(Long userId) {
        super("User with id "+userId+" not found");
    }
}
