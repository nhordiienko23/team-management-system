package com.nba.player;

import org.springframework.data.jpa.repository.*;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    @EntityGraph(attributePaths = {"team", "playerPositions"})
    List<Player> findAll();

    @EntityGraph(attributePaths = {"team","playerPositions"})
    List<Player> findAllByTeamId(Long teamId);
}