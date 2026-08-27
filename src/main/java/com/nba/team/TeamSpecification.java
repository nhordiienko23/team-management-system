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

            if (filter.name() != null && !filter.name().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("teamName")), "%" + filter.name().toLowerCase() + "%"));
            }

            if (filter.minChampionshipTitleCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("championshipTitleCount"), filter.minChampionshipTitleCount()));
            }

            if (filter.maxChampionshipTitleCount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("championshipTitleCount"), filter.maxChampionshipTitleCount()));
            }
            if (filter.creationDateStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creationDate"), filter.creationDateStart()));
            }
            if (filter.creationDateEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creationDate"), filter.creationDateEnd()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}
