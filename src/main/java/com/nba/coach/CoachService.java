package com.nba.coach;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;




public interface CoachService {
    ResponseCoachDto addCoach(RequestCoachDto dto);
    Page<ResponseCoachDto> getAllCoaches(Pageable pageable);
    ResponseCoachDto partialUpdateCoach(Long coachId, PatchCoachRequest dto);
    void deleteCoach(Long coachId);
    ResponseCoachDto getCoachById(Long coachId);
    Page<ResponseCoachDto> searchCoaches(CoachSearchFilter filter, Pageable pageable);
    TeamGroupResponse getColleaguesByCoachId(Long coachId);
    TeamTransferResponse changeCoachTeam(Long coachId, Long newTeamId);
}