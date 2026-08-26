package com.nba.core.mapper;

import com.nba.core.dto.response.MemberShortDto;
import com.nba.core.model.TeamMember;
import org.springframework.stereotype.Component;

@Component
public class MemberShortMapper {

    public MemberShortDto toMemberShortDto(TeamMember teamMember) {
        if (teamMember == null) return null;

        return MemberShortDto.builder()
                .id(teamMember.getId())
                .fullName(teamMember.getFirstName() + " " + teamMember.getLastName())
                .build();
    }
}