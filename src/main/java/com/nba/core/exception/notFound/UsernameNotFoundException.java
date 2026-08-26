package com.nba.core.exception.notFound;

public class UsernameNotFoundException extends ResourceNotFoundException {
    public UsernameNotFoundException(String message) {
        super(message);
    }
}
