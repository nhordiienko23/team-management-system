package com.nba.team;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    @EntityGraph(attributePaths = "teamMembers")
    List<Team> findAll();

    boolean existsByName(String name);
}
