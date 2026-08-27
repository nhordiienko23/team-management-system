package com.nba.team;

import com.nba.coach.Coach;
import com.nba.coach.CoachRepository;
import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.core.exception.invalidData.InvalidTeamDataException;
import com.nba.core.mapper.TeamTransferMapper;
import com.nba.player.Player;
import com.nba.player.PlayerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final PlayerRepository playerRepository;
    private final CoachRepository coachRepository;
    private final TeamTransferMapper teamTransferMapper;

    @Override
    @Transactional
    public ResponseTeamDto addTeam(RequestTeamDto dto) {
        if (teamRepository.existsByName(dto.name())) {
            throw new InvalidTeamDataException(
                    "Team with teamName " + dto.name() + " already exists");
        }

        Team team = teamMapper.toTeamEntity(dto);
        Team savedTeam = teamRepository.save(team);

        return teamMapper.toTeamDto(savedTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseTeamDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(teamMapper::toTeamDto)
                .toList();
    }

    @Override
    @Transactional
    public ResponseTeamDto partialUpdateTeam(Long teamId, PatchTeamRequest request) {
        Team team = teamRepository.getTeamByIdOrThrow404(teamId);

        if (request.teamName() != null) {
            if (!team.getName().equals(request.teamName())
                    && teamRepository.existsByName(request.teamName())) {
                throw new InvalidTeamDataException(
                        "Team with teamName " + request.teamName() + " already exists");
            }

            team.setName(request.teamName());
        }

        if (request.championshipCount() != null) {
            team.setChampionshipTitleCount(request.championshipCount());
        }

        return teamMapper.toTeamDto(team);
    }


    @Override
    @Transactional
    public void deleteTeam(Long teamId) {
        Team team = teamRepository.getTeamByIdOrThrow404(teamId);

        new ArrayList<>(team.getTeamMembers())
                .forEach(team::removeTeamMember);

        teamRepository.delete(team);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseTeamDto getTeamById(Long teamId) {
        return teamMapper.toTeamDto(teamRepository.getTeamByIdOrThrow404(teamId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseTeamDto> searchTeams(TeamSearchFilter filter) {
        return teamRepository.findAll(
                        TeamSpecification.buildQuery(filter))
                .stream()
                .map(teamMapper::toTeamDto)
                .toList();
    }

    @Override
    @Transactional
    public TeamTransferResponse addPlayerToTeam(Long teamId, Long playerId) {
        Player player = findPlayerByIdOrThrow400(playerId);
        Team newTeam = teamRepository.getTeamByIdOrThrow404(teamId);
        Team oldTeam = player.getTeam();
        Long currentTeamId = player.getTeam() != null
                ? player.getTeam().getId()
                : null;

        if (Objects.equals(currentTeamId, teamId)) {
            throw new InvalidTeamDataException(
                    "Player with id " + playerId
                            + " already exists in team with id " + teamId);
        }

        if (oldTeam != null) {
            oldTeam.removeTeamMember(player);
        }

        newTeam.addTeamMember(player);
        return teamTransferMapper.toTransferResponse(player,
                oldTeam,
                newTeam,
                (oldTeam != null)
                        ? "TRADE"
                        : "ADD");
    }

    @Override
    @Transactional
    public TeamTransferResponse addCoachToTeam(Long teamId, Long coachId) {
        Coach coach = findCoachByIdOrThrow400(coachId);
        Team newTeam = teamRepository.getTeamByIdOrThrow404(teamId);
        Team oldTeam = coach.getTeam();
        Long currentTeamId = coach.getTeam() != null
                ? coach.getTeam().getId()
                : null;

        if (Objects.equals(currentTeamId, teamId)) {
            throw new InvalidTeamDataException(
                    "Coach with id " + coachId
                            + " already exists in team with id " + teamId);
        }

        if (oldTeam != null) {
            oldTeam.removeTeamMember(coach);
        }
        newTeam.addTeamMember(coach);
        return teamTransferMapper.toTransferResponse(coach,
                oldTeam,
                newTeam,
                (oldTeam != null)
                        ? "TRADE"
                        : "ADD");
    }

    @Override
    @Transactional
    public TeamTransferResponse deletePlayerFromTeam(Long teamId, Long playerId) {
        Team team = teamRepository.getTeamByIdOrThrow404(teamId);
        Player player = playerRepository
                .findByIdAndTeamId(playerId, teamId)
                .orElseThrow(() -> new InvalidTeamDataException(
                        "Player with id " + playerId
                                + " doesn't exist in team with id " + teamId));

        team.removeTeamMember(player);
        return teamTransferMapper.toTransferResponse(
                player,
                team,
                null,
                "REMOVE"
        );
    }

    @Override
    @Transactional
    public TeamTransferResponse deleteCoachFromTeam(Long teamId, Long coachId) {
        Team team = teamRepository.getTeamByIdOrThrow404(teamId);
        Coach coach = coachRepository.findByIdAndTeamId(coachId, teamId)
                .orElseThrow(() -> new InvalidTeamDataException(
                        "Coach with id " + coachId
                                + " doesn't exist in team with id " + teamId));

        team.removeTeamMember(coach);
        return teamTransferMapper.toTransferResponse(coach,
                team,
                null,
                "REMOVE");
    }

    @Override
    @Transactional
    public void fireAllTeamMembers(Long teamId) {
        Team team = teamRepository.getTeamByIdOrThrow404(teamId);

        new ArrayList<>(team.getTeamMembers())
                .forEach(team::removeTeamMember);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamGroupResponse getTeamLineup(Long teamId) {
        return teamMapper.toPlayerLineup(
                teamRepository.getTeamByIdOrThrow404(teamId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TeamGroupResponse getCoachingStaff(Long teamId) {
        return teamMapper.toCoachingStaff(
                teamRepository.getTeamByIdOrThrow404(teamId)
        );
    }

    private Coach findCoachByIdOrThrow400(Long coachId) {
        return coachRepository.findById(coachId).orElseThrow(() ->
                new InvalidTeamDataException("Coach with id " + coachId + " not found"));
    }

    private Player findPlayerByIdOrThrow400(Long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() ->
                new InvalidTeamDataException("Player with id " + playerId + " not found"));
    }
}