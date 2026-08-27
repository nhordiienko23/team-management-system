package com.nba.coach;

import com.nba.core.dto.response.TeamGroupResponse;
import java.util.List;

public interface CoachService {
    ResponseCoachDto addCoach(RequestCoachDto dto);
    List<ResponseCoachDto> getAllCoaches();
    ResponseCoachDto partialUpdateCoach(Long coachId, PatchCoachRequest dto);
    void deleteCoach(Long coachId);
    ResponseCoachDto getCoachById(Long coachId);
    List<ResponseCoachDto> searchCoaches(CoachSearchFilter filter);
    TeamGroupResponse getColleaguesByCoachId(Long coachId);
    void changeCoachTeam(Long coachId, Long newTeamId);
}