package com.nba.coach;

import com.nba.core.exception.notFound.CoachNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface CoachRepository extends JpaRepository<Coach, Long>, JpaSpecificationExecutor<Coach> {

    @EntityGraph(attributePaths = "team")
    Page<Coach> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "team")
    List<Coach> findAllByTeamId(Long teamId);

    @EntityGraph(attributePaths = "team")
    Optional<Coach> findWithTeamById(Long coachId);

    @EntityGraph(attributePaths = "team")
    Optional<Coach> findByIdAndTeamId(Long coachId, Long teamId);

    default Coach getCoachByIdOrThrow404(Long coachId) {
        return findWithTeamById(coachId).orElseThrow(() -> new CoachNotFoundException(coachId));
    }


}