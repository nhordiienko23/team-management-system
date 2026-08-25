package com.nba.team;

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
    public ResponseTeamDto updateTeam(Long id, RequestTeamDto dto) {
        Team team = findTeamById(id);
        if (!team.getName().equals(dto.name()) && teamRepository.existsByName(dto.name())) {
            throw new InvalidTeamDataException("Team with name " + dto.name() + " already exists");
        }
        team.setName(dto.name());
        team.setChampionshipTitleCount(dto.championshipCount());

        return teamMapper.toTeamDto(team);
    }

    @Override
    @Transactional
    public void deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) throw new TeamNotFoundException(id);
        teamRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseTeamDto getTeamById(Long id) {
        Team team = teamRepository.findById(id).orElseThrow(() ->
                new TeamNotFoundException(id));
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
        Team team = findTeamById(teamId);
        if (team.getTeamMembers().contains(player))
            throw new InvalidTeamDataException("Player with id " + playerId + " already exists in team with id " + teamId);
        team.getTeamMembers().add(player);
        player.setTeam(team);
    }

    @Override
    @Transactional
    public void addCoachToTeam(Long teamId, Long coachId) {
        Coach coach = findCoachById(coachId);
        Team team = findTeamById(teamId);
        if (team.getTeamMembers().contains(coach))
            throw new InvalidTeamDataException("Coach with id " + coachId + " already exists in team with id " + teamId);
        team.getTeamMembers().add(coach);
        coach.setTeam(team);
    }

    @Override
    @Transactional
    public void deletePlayerFromTeam(Long teamId, Long playerId) {
        Team team = findTeamById(teamId);
        Player playerToRemove = (Player) team.getTeamMembers().stream()
                .filter(tm -> tm.getId().equals(playerId) && tm instanceof Player)
                .findFirst()
                .orElseThrow(() -> new InvalidTeamDataException("Player with id " + playerId + " doesn't exist in team with id " + teamId));
        team.getTeamMembers().remove(playerToRemove);
        playerToRemove.setTeam(null);
    }

    @Override
    @Transactional
    public void deleteCoachFromTeam(Long teamId, Long coachId) {
        Team team = findTeamById(teamId);
        Coach coachToRemove = (Coach) team.getTeamMembers().stream()
                .filter(tm -> tm.getId().equals(coachId) && tm instanceof Coach)
                .findFirst()
                .orElseThrow(() -> new InvalidTeamDataException("Coach with id " + coachId + " doesn't exist in team with id " + teamId));
        team.getTeamMembers().remove(coachToRemove);
        coachToRemove.setTeam(null);
    }

    @Override
    @Transactional
    public void fireAllTeamMembers(Long teamId) {
        Team team = findTeamById(teamId);
        team.getTeamMembers().forEach(teamMember -> teamMember.setTeam(null));
        team.getTeamMembers().clear();
    }

    private Player findPlayerById(Long playerId) {
        return playerRepository.findById(playerId).orElseThrow(() ->
                new InvalidTeamDataException("Player with id " + playerId + " not found"));
    }

    private Coach findCoachById(Long coachId) {
        return coachRepository.findById(coachId).orElseThrow(() ->
                new InvalidTeamDataException("Coach with id " + coachId + " not found"));
    }

    private Team findTeamById(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() ->
                new InvalidTeamDataException("Team with id " + teamId + " not found"));
    }
}