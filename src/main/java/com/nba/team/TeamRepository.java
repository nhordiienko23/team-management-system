package com.nba.team;

import com.nba.core.exception.notFound.TeamNotFoundException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {
    @Override
    @EntityGraph(attributePaths = "teamMembers")
    List<Team> findAll();

    boolean existsByName(String name);


    default Team getTeamByIdOrThrow404(Long teamId) {
        return findById(teamId).orElseThrow(() -> new TeamNotFoundException(teamId));
    }


}
