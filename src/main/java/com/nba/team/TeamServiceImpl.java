package com.nba.team;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.exception.invalidData.InvalidTeamDataException;
import com.nba.core.exception.notFound.TeamNotFoundException;
import com.nba.coach.Coach;
import com.nba.player.Player;
import com.nba.coach.CoachRepository;
import com.nba.player.PlayerRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final PlayerRepository playerRepository;
    private final CoachRepository coachRepository;

    @Override
    @Transactional
    public ResponseTeamDto addTeam(RequestTeamDto dto) {
        if (teamRepository.existsByName(dto.name())) {
            throw new InvalidTeamDataException("Team with name " + dto.name() + " already exists");
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
    public ResponseTeamDto updateTeam(Long teamId, RequestTeamDto dto) {
        Team team = teamRepository.getTeamByIdOrThrow(teamId);
        if (!team.getName().equals(dto.name()) && teamRepository.existsByName(dto.name())) {
            throw new InvalidTeamDataException("Team with name " + dto.name() + " already exists");
        }
        team.setName(dto.name());
        team.setChampionshipTitleCount(dto.championshipCount());

        return teamMapper.toTeamDto(team);
    }

    @Override
    @Transactional
    public void deleteTeam(Long teamId) {
        if (!teamRepository.existsById(teamId)) throw new TeamNotFoundException(teamId);
        teamRepository.deleteById(teamId);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseTeamDto getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() ->
                new TeamNotFoundException(teamId));
        return teamMapper.toTeamDto(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseTeamDto> searchTeams(TeamSearchFilter filter) {
        return teamRepository.findAll(TeamSpecification.buildQuery(filter)).stream()
                .map(teamMapper::toTeamDto)
                .toList();
    }

    @Override
    @Transactional
    public void addPlayerToTeam(Long teamId, Long playerId) {
        Player player = findPlayerById(playerId);
        Team team = teamRepository.getTeamByIdOrThrow(teamId);
        if (team.getTeamMembers().contains(player))
            throw new InvalidTeamDataException("Player with id " + playerId + " already exists in team with id " + teamId);
        team.getTeamMembers().add(player);
        player.setTeam(team);
    }

    @Override
    @Transactional
    public void addCoachToTeam(Long teamId, Long coachId) {
        Coach coach = findCoachById(coachId);
        Team team = teamRepository.getTeamByIdOrThrow(teamId);
        if (team.getTeamMembers().contains(coach))
            throw new InvalidTeamDataException("Coach with id " + coachId + " already exists in team with id " + teamId);
        team.getTeamMembers().add(coach);
        coach.setTeam(team);
    }

    @Override
    @Transactional
    public void deletePlayerFromTeam(Long teamId, Long playerId) {
        Team team = teamRepository.getTeamByIdOrThrow(teamId);
        Player playerToRemove = (Player) team.getPlayers().stream()
                .filter(tm -> tm.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() ->
                        new InvalidTeamDataException("Player with id " + playerId + " doesn't exist in team with id " + teamId));
        team.getTeamMembers().remove(playerToRemove);
        playerToRemove.setTeam(null);
    }

    @Override
    @Transactional
    public void deleteCoachFromTeam(Long teamId, Long coachId) {
        Team team = teamRepository.getTeamByIdOrThrow(teamId);
        Coach coachToRemove = (Coach) team.getCoaches().stream()
                .filter(tm -> tm.getId().equals(coachId))
                .findFirst()
                .orElseThrow(() ->
                        new InvalidTeamDataException("Coach with id " + coachId + " doesn't exist in team with id " + teamId));
        team.getTeamMembers().remove(coachToRemove);
        coachToRemove.setTeam(null);
    }


    @Override
    @Transactional
    public void fireAllTeamMembers(Long teamId) {
        Team team = teamRepository.getTeamByIdOrThrow(teamId);
        team.getTeamMembers().forEach(teamMember -> teamMember.setTeam(null));
        team.getTeamMembers().clear();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamGroupResponse getTeamLineup(Long teamId) {
        return teamMapper.toPlayerLineup(teamRepository.getTeamByIdOrThrow(teamId));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamGroupResponse getCoachingStaff(Long teamId) {
        return teamMapper.toCoachingStaff(teamRepository.getTeamByIdOrThrow(teamId));
    }


    private Player findPlayerById(Long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() ->
                new InvalidTeamDataException("Player with id " + playerId + " not found"));
    }

    private Coach findCoachById(Long coachId) {
        return coachRepository.findById(coachId).orElseThrow(() ->
                new InvalidTeamDataException("Coach with id " + coachId + " not found"));
    }


}