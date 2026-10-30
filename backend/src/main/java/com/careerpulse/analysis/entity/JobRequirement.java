package com.careerpulse.analysis.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "job_requirements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_requirement",
                columnNames = {"job_id", "requirement_key"}
        )
)
public class JobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requirement_id")
    private Long requirementId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "requirement_key", nullable = false, length = 50)
    private String requirementKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 20)
    private RequirementCategory requirementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "comparison_kind", nullable = false, length = 20)
    private ComparisonKind comparisonKind;

    @Column(name = "requirement_name", nullable = false, length = 255)
    private String requirementName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_quote", columnDefinition = "TEXT")
    private String sourceQuote;

    @Column(name = "evaluation_rule", length = 500)
    private String evaluationRule;

    protected JobRequirement() {
    }

    public JobRequirement(
            Long jobId,
            String requirementKey,
            RequirementCategory requirementType,
            ComparisonKind comparisonKind,
            String requirementName,
            String description,
            String sourceQuote
    ) {
        this(
                jobId,
                requirementKey,
                requirementType,
                comparisonKind,
                requirementName,
                description,
                sourceQuote,
                null
        );
    }

    public JobRequirement(
            Long jobId,
            String requirementKey,
            RequirementCategory requirementType,
            ComparisonKind comparisonKind,
            String requirementName,
            String description,
            String sourceQuote,
            String evaluationRule
    ) {
        this.jobId = jobId;
        this.requirementKey = requirementKey;
        apply(
                requirementType,
                comparisonKind,
                requirementName,
                description,
                sourceQuote,
                evaluationRule
        );
    }

    public void apply(
            RequirementCategory requirementType,
            ComparisonKind comparisonKind,
            String requirementName,
            String description,
            String sourceQuote
    ) {
        apply(
                requirementType,
                comparisonKind,
                requirementName,
                description,
                sourceQuote,
                null
        );
    }

    public void apply(
            RequirementCategory requirementType,
            ComparisonKind comparisonKind,
            String requirementName,
            String description,
            String sourceQuote,
            String evaluationRule
    ) {
        this.requirementType = requirementType;
        this.comparisonKind = comparisonKind;
        this.requirementName = requirementName;
        this.description = description;
        this.sourceQuote = sourceQuote;
        if (evaluationRule != null && !evaluationRule.isBlank()) {
            this.evaluationRule = evaluationRule;
        }
    }

    public Long getRequirementId() { return requirementId; }
    public Long getJobId() { return jobId; }
    public String getRequirementKey() { return requirementKey; }
    public RequirementCategory getRequirementType() { return requirementType; }
    public ComparisonKind getComparisonKind() { return comparisonKind; }
    public String getRequirementName() { return requirementName; }
    public String getDescription() { return description; }
    public String getSourceQuote() { return sourceQuote; }
    public String getEvaluationRule() { return evaluationRule; }
}
