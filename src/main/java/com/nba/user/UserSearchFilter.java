package com.nba.user;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.Set;

public record UserSearchFilter(
        String username,
        String email,
        Set<UserRole> roles,


        @DateTimeFormat(pattern = "dd.MM.yyyy")
        LocalDate registeredAtStart,

        @DateTimeFormat(pattern = "dd.MM.yyyy")
        LocalDate registeredAtEnd,

        @DateTimeFormat(pattern = "dd.MM.yyyy")
        LocalDate lastLoginStart,

        @DateTimeFormat(pattern = "dd.MM.yyyy")
        LocalDate lastLoginEnd
) {
}