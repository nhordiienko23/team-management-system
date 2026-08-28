package com.nba.team;

import com.nba.core.exception.notFound.TeamNotFoundException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {
    @Override
    @EntityGraph(attributePaths = "teamMembers")
    List<Team> findAll();

    boolean existsByName(String name);

    @EntityGraph(attributePaths = "teamMembers")
    @Query("SELECT t FROM Team t WHERE t.id = :teamId")
    Optional<Team> findByIdWithTeamMembers(@Param("teamId") Long teamId);

    default Team getTeamByIdOrThrow404(Long teamId) {
        return findByIdWithTeamMembers(teamId).orElseThrow(() -> new TeamNotFoundException(teamId));
    }


}
