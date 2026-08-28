package com.nba.player;

import com.nba.core.exception.notFound.PlayerNotFoundException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    @EntityGraph(attributePaths = {"team", "playerPositions"})
    List<Player> findAll();

    @EntityGraph(attributePaths = {"team", "playerPositions"})
    List<Player> findAllByTeamId(Long teamId);

    @EntityGraph(attributePaths = {"team", "playerPositions"})
    Optional<Player> findPlayerWithTeamAndPositionsById(Long playerId);

    @EntityGraph(attributePaths = {"team", "playerPositions"})
    Optional<Player> findByIdAndTeamId(Long playerId, Long teamId);

    default Player getPlayerByIdOrThrow404(Long playerId) {
        return findPlayerWithTeamAndPositionsById(playerId).orElseThrow(() -> new PlayerNotFoundException(playerId));
    }


}