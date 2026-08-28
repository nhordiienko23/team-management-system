package com.nba.user;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> buildQuery(UserSearchFilter filter) {
        return ((root, query, cb) -> {

            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("roles", JoinType.LEFT);
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.username() != null && !filter.username().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + filter.username().toLowerCase() + "%"));
            }

            if (filter.email() != null && !filter.email().isBlank()) {

                predicates.add(cb.like(cb.lower(root.get("email")), "%" + filter.email().toLowerCase() + "%"));
            }

            if (filter.roles() != null && !filter.roles().isEmpty()) {
                predicates.add(root.join("roles", JoinType.LEFT).in(filter.roles()));
            }

            if (filter.registeredAtStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("registeredAt"), filter.registeredAtStart().atStartOfDay()));
            }
            if (filter.registeredAtEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("registeredAt"), filter.registeredAtEnd().atTime(LocalTime.MAX)));
            }

            if (filter.lastLoginStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastLogin"), filter.lastLoginStart().atStartOfDay()));
            }
            if (filter.lastLoginEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastLogin"), filter.lastLoginEnd().atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}