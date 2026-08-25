package com.nba.coach;

import org.springframework.stereotype.Component;

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
}
