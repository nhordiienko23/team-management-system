package com.nba.coach;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
 public interface CoachRepository extends JpaRepository<Coach, Long>, JpaSpecificationExecutor<Coach> {

    @EntityGraph(attributePaths = "team")
    List<Coach> findAll();


}