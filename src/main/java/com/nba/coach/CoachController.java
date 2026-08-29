package com.nba.coach;

import com.nba.core.dto.response.MessageResponse;
import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coaches")
@AllArgsConstructor
@Tag(name = "coach API", description = "Endpoints for managing coach information")
public class CoachController {

    private final CoachService coachService;

    @Operation(summary = "returns coach by id")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseCoachDto> getCoachById(@PathVariable Long id) {
        return ResponseEntity.ok(coachService.getCoachById(id));
    }

    @Operation(summary = "returns list of all coaches")
    @GetMapping
    public List<ResponseCoachDto> getAllCoaches() {
        return coachService.getAllCoaches();
    }

    @Operation(summary = "Returns list of coaches flexibly filtered by any combination of parameters")
    @GetMapping("/search")
    public List<ResponseCoachDto> searchCoaches(
            @ParameterObject CoachSearchFilter filter) {
        return coachService.searchCoaches(filter);
    }

    @Operation(summary = "returns list of colleagues")
    @GetMapping("/{coachId}/colleagues")
    public ResponseEntity<TeamGroupResponse> getColleagues(@PathVariable Long coachId) {
        return ResponseEntity.ok(coachService.getColleaguesByCoachId(coachId));
    }
}