package com.nba.coach;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

 class CoachSpecification {

    public static Specification<Coach> buildQuery(CoachSearchFilter filter) {
        return ((root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("team", JoinType.LEFT);
            }

            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.firstName() != null && !filter.firstName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")),
                        "%" + filter.firstName().toLowerCase() + "%"));
            }

            if (filter.lastName() != null && !filter.lastName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")),
                        "%" + filter.lastName().toLowerCase() + "%"));
            }

            if (filter.teamName() != null && !filter.teamName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.join("team", JoinType.LEFT).get("teamName"))
                        , "%" + filter.teamName().toLowerCase() + "%"));
            }

            if (filter.minChampionshipsWon() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("championshipsWon"),
                        filter.minChampionshipsWon()));
            }

            if (filter.maxChampionshipsWon() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("championshipsWon"),
                        filter.maxChampionshipsWon()));
            }

            if (filter.minYearsOfExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearsOfExperience"),
                        filter.minYearsOfExperience()));
            }

            if (filter.maxYearsOfExperience() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("yearsOfExperience"),
                        filter.maxYearsOfExperience()));
            }
            if (filter.minSalary() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salary"),
                        filter.minSalary()));
            }

            if (filter.maxSalary() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salary"),
                        filter.maxSalary()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

}
