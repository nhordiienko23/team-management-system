package com.nba.team;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

class TeamSpecification {
    public static Specification<Team> buildQuery(TeamSearchFilter filter) {
        return ((root, query, cb) -> {

            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("teamMembers", JoinType.LEFT);
            }
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.teamName() != null && !filter.teamName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.teamName().toLowerCase() + "%"));
            }

            if (filter.minChampionshipTitleCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("championshipTitleCount"), filter.minChampionshipTitleCount()));
            }

            if (filter.maxChampionshipTitleCount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("championshipTitleCount"), filter.maxChampionshipTitleCount()));
            }

            if (filter.minCreationYear() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creationYear"), filter.minCreationYear()));
            }
            if (filter.maxCreationYear() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creationYear"), filter.maxCreationYear()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}