package com.nba.team;

import com.nba.core.dto.response.MemberShortDto;
import lombok.Builder;

import java.util.List;
@Builder
public record ResponseGroupType(String teamName,
                                TeamGroupType groupType,
                                List<MemberShortDto> members) {

}
