package com.nba.coach;

import com.nba.core.dto.response.TeamGroupResponse;
import java.util.List;

public interface CoachService {
    ResponseCoachDto addCoach(RequestCoachDto dto);
    List<ResponseCoachDto> getAllCoaches();
    ResponseCoachDto updateCoach(Long coachId, RequestCoachDto dto);
    void deleteCoach(Long coachId);
    ResponseCoachDto getCoachById(Long coachId);
    List<ResponseCoachDto> searchCoaches(CoachSearchFilter filter);
    TeamGroupResponse getColleaguesByCoachId(Long coachId);
}