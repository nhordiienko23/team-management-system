package com.nba.core.mapper;

import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.core.model.TeamMember;
import com.nba.team.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamTransferMapper {

    public TeamTransferResponse toTransferResponse(TeamMember member, Team oldTeam, Team newTeam, String operationType) {
        String role = member.getClass().getSimpleName().toUpperCase();
        String memberFullName = member.getFirstName() + " " + member.getLastName();

        String oldTeamName = (oldTeam != null) ? oldTeam.getName() : "Free Agent";
        String newTeamName = (newTeam != null) ? newTeam.getName() : "Free Agent";
        Long newTeamId = (newTeam != null) ? newTeam.getId() : null;

        String message = generateMessage(operationType, role, memberFullName, oldTeamName, newTeamName);

        return TeamTransferResponse.builder()
                .message(message)
                .memberId(member.getId())
                .memberFullName(memberFullName)
                .memberRole(role)
                .oldTeamName(oldTeamName)
                .newTeamId(newTeamId)
                .newTeamName(newTeamName)
                .build();
    }

    private String generateMessage(String operation, String role, String name, String oldTeam, String newTeam) {
        return switch (operation.toUpperCase()) {
            case "ADD" -> String.format("Successfully added %s %s to %s.", role, name, newTeam);
            case "REMOVE" ->
                    String.format("Successfully removed %s %s from %s. Now %s is a Free Agent.", role, name, oldTeam,name);
            case "TRADE" -> (oldTeam.equals("Free Agent"))
                    ? String.format("Successfully traded %s %s to %s.", role, name, newTeam)
                    : String.format("Successfully traded %s %s from %s to %s.", role, name, oldTeam, newTeam) ;
            default -> "Operation completed successfully.";
        };
    }
}