package com.nba.coach;

import com.nba.core.dto.response.MessageResponse;
import com.nba.player.ResponseTeammates;
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
@Tag(name = "coach API")
public class CoachController {
    private final CoachService coachService;

    @Operation(summary = "creates new coach")
    @PostMapping
    public ResponseEntity<ResponseCoachDto> addCoach(@Valid @RequestBody RequestCoachDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coachService.addCoach(dto));
    }

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

    @Operation(summary = "updates coach by id")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseCoachDto> updateCoach(@PathVariable Long id,
                                                        @Valid @RequestBody RequestCoachDto dto) {
        return ResponseEntity.ok(coachService.updateCoach(id, dto));
    }

    @Operation(summary = "deletes coach by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCoach(@PathVariable Long id) {
        coachService.deleteCoach(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Coach with id " + id + " was deleted successfully")
                .build());
    }
    @Operation(summary = "returns list of colleagues")
    @GetMapping("/{coachId}/colleagues")
    public ResponseEntity<ResponseColleagues> getTeammates(@PathVariable Long coachId){
        return  ResponseEntity.ok(coachService.getColleaguesByCoachId(coachId));
    }


}
