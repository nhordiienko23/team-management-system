package com.nba.coach;


 interface CoachMapper {
    Coach toCoachEntity(RequestCoachDto dto);

    ResponseCoachDto toCoachDto(Coach coach);
}
