package com.nba.user;


public interface UserMapper {
    UserDto toUserDto(User user);
    ResponseSearchUser toResponseSearchUser(User user);
}
