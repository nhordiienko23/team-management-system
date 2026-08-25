package com.nba.coach;


import com.nba.core.dto.response.MemberShortDto;

import java.util.List;

interface CoachMapper {
    Coach toCoachEntity(RequestCoachDto dto);

    ResponseCoachDto toCoachDto(Coach coach);
    MemberShortDto toMemberShortDto(Coach coach);
    ResponseColleagues toColleaguesDto(List<Coach> coaches, Coach coach);
}
