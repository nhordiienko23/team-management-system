package com.nba.coach;

import com.nba.core.dto.response.MemberShortDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
class CoachMapperImpl implements CoachMapper {
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
    public MemberShortDto toMemberShortDto(Coach coach) {
        return MemberShortDto.builder()
                .id(coach.getId())
                .fullName(coach.getFirstName() + " " + coach.getLastName())
                .build();
    }

    @Override
    public ResponseColleagues toColleaguesDto(List<Coach> coaches, Coach coach) {
        if (coaches == null || coaches.isEmpty()) return ResponseColleagues.
                builder()
                .colleagues(Collections.emptyList())
                .build();
        String teamName = coaches.get(0).getTeam().getName();
        return ResponseColleagues.builder()
                .teamName(teamName)
                .coachFullName(coach.getFirstName() + " " + coach.getLastName())
                .colleagues(coaches.stream()
                        .filter(p -> !p.getId().equals(coach.getId()))
                        .map(this::toMemberShortDto)
                        .toList())
                .build();
    }


}
