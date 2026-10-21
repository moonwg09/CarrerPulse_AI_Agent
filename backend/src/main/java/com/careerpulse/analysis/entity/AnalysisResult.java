package com.careerpulse.analysis.entity;

import com.careerpulse.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 서류(이력서/자기소개서)와 공고 1건을 비교한 결과.
 *
 * 정렬/필터에 쓰는 값은 컬럼으로 풀어서 저장하고,
 * 근거 원문을 포함한 AI 응답 전체는 rawResult 에 원본 그대로 보관한다.
 */
@Entity
@Table(
        name = "analysis_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_analysis_user_job",
                        columnNames = {"user_id", "job_key"}
                )
        }
)
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_key", nullable = false, length = 100)
    private String jobKey;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_status", length = 30)
    private RecommendationStatus recommendationStatus;

    @Column(name = "overall_match_rate", precision = 5, scale = 2)
    private BigDecimal overallMatchRate;

    @Column(name = "required_match_rate", precision = 5, scale = 2)
    private BigDecimal requiredMatchRate;

    @Column(name = "preferred_match_rate", precision = 5, scale = 2)
    private BigDecimal preferredMatchRate;

    @Column(name = "objective_match_rate", precision = 5, scale = 2)
    private BigDecimal objectiveMatchRate;

    @Column(name = "evaluation_coverage", precision = 5, scale = 2)
    private BigDecimal evaluationCoverage;

    @Lob
    @Column(name = "warnings", columnDefinition = "LONGTEXT")
    private String warnings;

    @Lob
    @Column(name = "raw_result", nullable = false, columnDefinition = "LONGTEXT")
    private String rawResult;

    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "analysisResult",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("itemOrder ASC")
    private List<AnalysisItem> items = new ArrayList<>();

    protected AnalysisResult() {
    }

    public AnalysisResult(
            User user,
            String jobKey
    ) {
        this.user = user;
        this.jobKey = jobKey;
    }

    /**
     * 같은 공고를 다시 비교했을 때 기존 행을 덮어쓴다.
     * 요구사항 목록은 비우고 새로 담는다.
     */
    public void apply(
            String jobTitle,
            String companyName,
            LocalDate closeDate,
            RecommendationStatus recommendationStatus,
            BigDecimal overallMatchRate,
            BigDecimal requiredMatchRate,
            BigDecimal preferredMatchRate,
            BigDecimal objectiveMatchRate,
            BigDecimal evaluationCoverage,
            String warnings,
            String rawResult
    ) {
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.closeDate = closeDate;
        this.recommendationStatus = recommendationStatus;
        this.overallMatchRate = overallMatchRate;
        this.requiredMatchRate = requiredMatchRate;
        this.preferredMatchRate = preferredMatchRate;
        this.objectiveMatchRate = objectiveMatchRate;
        this.evaluationCoverage = evaluationCoverage;
        this.warnings = warnings;
        this.rawResult = rawResult;

        this.items.clear();
    }

    public void addItem(AnalysisItem item) {
        this.items.add(item);
        item.assignTo(this);
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public User getUser() {
        return user;
    }

    public String getJobKey() {
        return jobKey;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public LocalDate getCloseDate() {
        return closeDate;
    }

    public RecommendationStatus getRecommendationStatus() {
        return recommendationStatus;
    }

    public BigDecimal getOverallMatchRate() {
        return overallMatchRate;
    }

    public BigDecimal getRequiredMatchRate() {
        return requiredMatchRate;
    }

    public BigDecimal getPreferredMatchRate() {
        return preferredMatchRate;
    }

    public BigDecimal getObjectiveMatchRate() {
        return objectiveMatchRate;
    }

    public BigDecimal getEvaluationCoverage() {
        return evaluationCoverage;
    }

    public String getWarnings() {
        return warnings;
    }

    public String getRawResult() {
        return rawResult;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<AnalysisItem> getItems() {
        return items;
    }
}
