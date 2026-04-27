package com.nba.repository;

import com.nba.model.Staff;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    // Solves the N+1 problem by fetching player positions in a single query
    @Query("SELECT s FROM Staff s LEFT JOIN FETCH TREAT(s AS Player).positions")
    List<Staff> findAllWithPositions(Sort sort);
}