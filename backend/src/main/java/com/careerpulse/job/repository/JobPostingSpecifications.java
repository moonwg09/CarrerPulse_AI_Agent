package com.careerpulse.job.repository;

import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.entity.JobPostingStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JobPostingSpecifications {

    private JobPostingSpecifications() {
    }

    public static Specification<JobPosting> search(
            List<String> interestTerms,
            String region,
            String keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(criteriaBuilder.equal(
                    root.get("status"),
                    JobPostingStatus.OPEN
            ));
            conditions.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("deadline")),
                    criteriaBuilder.greaterThanOrEqualTo(
                            root.<LocalDate>get("deadline"),
                            LocalDate.now()
                    )
            ));

            Expression<String> title = criteriaBuilder.lower(root.get("title"));
            Expression<String> description = criteriaBuilder.lower(root.get("description"));
            List<Predicate> interestConditions = interestTerms.stream()
                    .filter(JobPostingSpecifications::hasText)
                    .map(term -> containsEither(
                            criteriaBuilder,
                            title,
                            description,
                            normalizeInterestKeyword(term)
                    ))
                    .toList();
            conditions.add(criteriaBuilder.or(
                    interestConditions.toArray(Predicate[]::new)
            ));
            if (!"전국".equals(region.trim())) {
                conditions.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("region")),
                        likePattern(region),
                        '\\'
                ));
            }

            if (hasText(keyword)) {
                Expression<String> companyName = criteriaBuilder.lower(
                        root.get("companyName")
                );
                String pattern = likePattern(keyword);
                conditions.add(criteriaBuilder.or(
                        criteriaBuilder.like(title, pattern, '\\'),
                        criteriaBuilder.like(companyName, pattern, '\\'),
                        criteriaBuilder.like(description, pattern, '\\')
                ));
            }

            return criteriaBuilder.and(conditions.toArray(Predicate[]::new));
        };
    }

    static String normalizeInterestKeyword(String jobName) {
        String normalized = jobName == null ? "" : jobName.trim();
        normalized = normalized.replaceFirst("\\s*개발자$", "").trim();
        return normalized.isBlank() ? "개발" : normalized;
    }

    private static Predicate containsEither(
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            Expression<String> first,
            Expression<String> second,
            String value
    ) {
        String pattern = likePattern(value);
        return criteriaBuilder.or(
                criteriaBuilder.like(first, pattern, '\\'),
                criteriaBuilder.like(second, pattern, '\\')
        );
    }

    private static String likePattern(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
