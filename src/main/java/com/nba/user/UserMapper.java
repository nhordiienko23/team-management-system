package com.nba.user;


public interface UserMapper {
    UserShortDto toUserShortDto(User user);
    UserFullDto toUserFullDto(User user);
    User toUserEntity(UserCreationRequest request);

}
