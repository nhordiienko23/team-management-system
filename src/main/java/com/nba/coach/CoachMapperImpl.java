package com.nba.coach;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.mapper.MemberShortMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
class CoachMapperImpl implements CoachMapper {

    private final MemberShortMapper memberShortMapper;

    @Override
    public Coach toCoachEntity(RequestCoachDto dto) {
        if (dto == null) return null;

        return Coach.builder()
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .salary(dto.salary())
                .yearsOfExperience(dto.yearsOfExperience())
                .championshipsWon(dto.championshipsWon())
                .build();
    }

    @Override
    public ResponseCoachDto toCoachDto(Coach coach) {
        if (coach == null) return null;
        String teamName = coach.getTeam() != null ?
                coach.getTeam().getName()
                : null;

        return ResponseCoachDto.builder()
                .id(coach.getId())
                .firstName(coach.getFirstName())
                .lastName(coach.getLastName())
                .salary(coach.getSalary())
                .teamName(teamName)
                .teamRole("Coach")
                .yearsExperience(coach.getYearsOfExperience())
                .championshipWon(coach.getChampionshipsWon())
                .build();
    }

    @Override
    public TeamGroupResponse toColleaguesDto(List<Coach> coaches, Coach coach) {
        if (coaches == null || coaches.isEmpty()) return TeamGroupResponse.builder()
                .members(Collections.emptyList())
                .build();

        String teamName = coaches.get(0).getTeam().getName();
        return TeamGroupResponse.builder()
                .teamName(teamName)
                .title("COLLEAGUES OF " + coach.getFirstName() + " " + coach.getLastName())
                .members(coaches.stream()
                        .filter(p -> !p.getId().equals(coach.getId()))
                        .map(memberShortMapper::toMemberShortDto)
                        .toList())
                .build();
    }
}