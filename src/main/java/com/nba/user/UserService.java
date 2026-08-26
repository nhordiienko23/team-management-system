package com.nba.user;

public interface UserService {
    User getUserByUsername(String username);
    UserDto saveToDataBase(User user);
    boolean existsByUsername(String username);
}
