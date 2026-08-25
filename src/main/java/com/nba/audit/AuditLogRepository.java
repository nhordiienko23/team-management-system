package com.nba.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
 interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}