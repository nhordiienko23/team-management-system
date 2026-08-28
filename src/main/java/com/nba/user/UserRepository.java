package com.nba.user;

import com.nba.core.exception.notFound.UserNotFoundException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);


    List<User> findAll();


    @EntityGraph(attributePaths = "roles")
    @Query("Select u FROM User u")
    List<User> findAllWithRoles();

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long userId);

    default User findUserByIdOrThrow404(Long userId){
        return findWithRolesById(userId).orElseThrow(()->new UserNotFoundException(userId));
    }
}
