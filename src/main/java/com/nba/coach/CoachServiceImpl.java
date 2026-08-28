package com.nba.coach;

import com.nba.core.dto.response.TeamGroupResponse;
import com.nba.core.dto.response.TeamTransferResponse;
import com.nba.core.exception.invalidData.InvalidCoachDataException;
import com.nba.core.mapper.TeamTransferMapper;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
class CoachServiceImpl implements CoachService {
    private final CoachMapper coachMapper;
    private final CoachRepository coachRepository;
    private final TeamRepository teamRepository;
    private final TeamTransferMapper teamTransferMapper;

    @Override
    @Transactional
    public ResponseCoachDto addCoach(RequestCoachDto dto) {
        Coach coach = coachMapper.toCoachEntity(dto);
        if (dto.teamId() != null) {
            Team team = findTeamByIdOrThrow400(dto.teamId());
            team.addTeamMember(coach);
        }
        return coachMapper.toCoachDto(coachRepository.save(coach));
    }

    @Override
    @Transactional
    public ResponseCoachDto partialUpdateCoach(Long coachId, PatchCoachRequest request) {
        Coach coach = coachRepository.getCoachByIdOrThrow404(coachId);

        if (request.firstName() != null) coach.setFirstName(request.firstName());
        if (request.lastName() != null) coach.setLastName(request.lastName());
        if (request.salary() != null) coach.setSalary(request.salary());
        if (request.yearsOfExperience() != null) coach.setYearsOfExperience(request.yearsOfExperience());
        if (request.championshipsWon() != null) coach.setChampionshipsWon(request.championshipsWon());

        if (request.teamId() != null) {
            Team newTeam = findTeamByIdOrThrow400(request.teamId());
            if (coach.getTeam() != null) {
                coach.getTeam().removeTeamMember(coach);
            }
            newTeam.addTeamMember(coach);
        }

        return coachMapper.toCoachDto(coach);
    }

    @Override
    @Transactional
    public void deleteCoach(Long coachId) {
        Coach coach = coachRepository.getCoachByIdOrThrow404(coachId);
        Team oldTeam = coach.getTeam();
        if (oldTeam != null) {
            oldTeam.removeTeamMember(coach);
        }
        coachRepository.delete(coach);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseCoachDto> getAllCoaches() {
        return coachRepository.findAll().stream()
                .map(coachMapper::toCoachDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseCoachDto getCoachById(Long coachId) {
        return coachMapper.toCoachDto(coachRepository.getCoachByIdOrThrow404(coachId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseCoachDto> searchCoaches(CoachSearchFilter filter) {
        return coachRepository.findAll(CoachSpecification.buildQuery(filter)).stream()
                .map(coachMapper::toCoachDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamGroupResponse getColleaguesByCoachId(Long coachId) {
        Coach coach = coachRepository.getCoachByIdOrThrow404(coachId);
        if (coach.getTeam() == null) {
            throw new InvalidCoachDataException("Coach with id " + coachId + " doesn't work in any team and has no colleagues");
        }
        Long teamId = coach.getTeam().getId();
        return coachMapper.toColleaguesDto(coachRepository.findAllByTeamId(teamId), coach);
    }

    @Override
    @Transactional
    public TeamTransferResponse changeCoachTeam(Long coachId, Long newTeamId) {
        Coach coach = coachRepository.getCoachByIdOrThrow404(coachId);
        Team oldTeam = coach.getTeam();
        Long currentTeamId = coach.getTeam() != null
                ? coach.getTeam().getId()
                : null;

        if (Objects.equals(currentTeamId, newTeamId)) {
            throw new InvalidCoachDataException(
                    newTeamId == null
                            ? "Coach is already a free agent"
                            : "Coach is already a team member of this team");
        }

        Team newTeam = null;

        if (newTeamId != null) {
            newTeam = findTeamByIdOrThrow400(newTeamId);
        }

        if (oldTeam != null) {
            oldTeam.removeTeamMember(coach);
        }

        if (newTeam != null) {
            newTeam.addTeamMember(coach);
        }
        return teamTransferMapper.toTransferResponse(coach,
                oldTeam,
                newTeam,
                (newTeam != null)
                        ? "TRADE"
                        : "REMOVE");
    }

    private Team findTeamByIdOrThrow400(Long teamId) {
        return teamRepository.findByIdWithTeamMembers(teamId).orElseThrow(() ->
                new InvalidCoachDataException("Team with id " + teamId + " not found"));
    }
}