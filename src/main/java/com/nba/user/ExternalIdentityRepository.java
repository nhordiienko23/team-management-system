package com.nba.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<ExternalIdentity> findByProviderAndSubject(String provider, String subject);

    boolean existsByUser_IdAndProvider(Long userId, String provider);

    List<ExternalIdentity> findAllByUser_Id(Long userId);

    List<ExternalIdentity> findAllByUser_IdIn(Collection<Long> userIds);
}
