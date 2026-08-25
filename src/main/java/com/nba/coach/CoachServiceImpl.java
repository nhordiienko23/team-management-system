package com.nba.coach;

import com.nba.core.exception.notFound.CoachNotFoundException;
import com.nba.core.exception.invalidData.InvalidCoachDataException;
import com.nba.team.Team;
import com.nba.team.TeamRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@AllArgsConstructor
 class CoachServiceImpl implements CoachService {
    private final CoachMapper coachMapper;
    private final CoachRepository coachRepository;
    private final TeamRepository teamRepository;

    @Override
    @Transactional
    public ResponseCoachDto addCoach(RequestCoachDto dto) {
        Coach coach = coachMapper.toCoachEntity(dto);
        if (dto.teamId() != null) {
            coach.setTeam(findTeamById(dto.teamId()));
        }
        Coach savedCoach = coachRepository.save(coach);
        return coachMapper.toCoachDto(savedCoach);
    }

    @Override
    @Transactional
    public ResponseCoachDto updateCoach(Long id, RequestCoachDto dto) {
        Coach coach = findCoachById(id);

        coach.setFirstName(dto.firstName());
        coach.setLastName(dto.lastName());
        coach.setSalary(dto.salary());
        coach.setYearsOfExperience(dto.yearsOfExperience());
        coach.setChampionshipsWon(dto.championshipsWon());
        if (dto.teamId() != null) {
            coach.setTeam(findTeamById(dto.teamId()));
        }else{
            coach.setTeam(null);
        }

        return coachMapper.toCoachDto(coach);
    }

    @Override
    @Transactional
    public void deleteCoach(Long id) {
        if (!coachRepository.existsById(id)) throw new CoachNotFoundException(id);
        coachRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseCoachDto> getAllCoaches() {
        return coachRepository.findAll().stream().
                map(coachMapper::toCoachDto).
                toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseCoachDto getCoachById(Long id) {
        return coachMapper.toCoachDto(findCoachById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseCoachDto> searchCoaches(CoachSearchFilter filter) {
        return coachRepository.findAll(CoachSpecification.buildQuery(filter)).stream()
                .map(coachMapper::toCoachDto)
                .toList();
    }

    private Coach findCoachById(Long coachId){
        return coachRepository.findById(coachId).orElseThrow(() ->
                new CoachNotFoundException(coachId));
    }
    private  Team findTeamById(Long teamId){
        return teamRepository.findById(teamId).orElseThrow(() ->
                new InvalidCoachDataException("team with id " + teamId + " not found"));
    }

}
