package com.nba.team;

import com.nba.core.exception.notFound.TeamNotFoundException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    @EntityGraph(attributePaths = "teamMembers")
    List<Team> findAll();

    boolean existsByName(String name);

    default Team getTeamByIdOrThrow(Long teamId){
        return findById(teamId).orElseThrow(()-> new TeamNotFoundException(teamId));
    }
}
