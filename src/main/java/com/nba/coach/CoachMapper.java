package com.nba.coach;

import com.nba.core.dto.response.TeamGroupResponse;
import java.util.List;

interface CoachMapper {
    Coach toCoachEntity(RequestCoachDto dto);
    ResponseCoachDto toCoachDto(Coach coach);
    TeamGroupResponse toColleaguesDto(List<Coach> coaches, Coach coach);
}