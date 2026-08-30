package com.nba.player;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

 class PlayerSpecification {

    public static Specification<Player> buildQuery(PlayerSearchFilter filter) {
        return (root, query, cb) -> {

            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("team", JoinType.LEFT);
                root.fetch("playerPositions", JoinType.LEFT);
            }
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.firstName() != null && !filter.firstName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("firstName")), "%" + filter.firstName().toLowerCase() + "%"));
            }

            if (filter.lastName() != null && !filter.lastName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + filter.lastName().toLowerCase() + "%"));
            }

            if (filter.teamName() != null && !filter.teamName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.join("team", JoinType.LEFT).get("name")), "%" + filter.teamName().toLowerCase() + "%"));
            }

            if (filter.minRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filter.minRating()));
            }

            if (filter.maxRating() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), filter.maxRating()));
            }

            if (filter.minChampionshipWon() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("championshipsWon"), filter.minChampionshipWon()));
            }

            if (filter.maxChampionshipWon() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("championshipsWon"), filter.maxChampionshipWon()));
            }


            if (filter.positions() != null && !filter.positions().isEmpty()) {
                List<PlayerPosition> targetPositions = filter.positions().stream()
                        .filter(pos -> pos != null && !pos.isBlank())
                        .flatMap(pos -> PlayerPosition.findByPartialName(pos).stream())
                        .distinct()
                        .toList();

                if (targetPositions.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.join("playerPositions", JoinType.LEFT).in(targetPositions));
                }
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
        };
    }
}