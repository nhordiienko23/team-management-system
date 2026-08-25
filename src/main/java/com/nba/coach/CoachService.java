package com.nba.coach;

import java.util.List;

public interface CoachService {
    ResponseCoachDto addCoach(RequestCoachDto dto);

    List<ResponseCoachDto> getAllCoaches();
    ResponseCoachDto updateCoach(Long id, RequestCoachDto dto);
    void deleteCoach(Long id);
    ResponseCoachDto getCoachById(Long id);
    List<ResponseCoachDto> searchCoaches(CoachSearchFilter filter);
    ResponseColleagues getColleaguesByCoachId(Long coachId);
}
