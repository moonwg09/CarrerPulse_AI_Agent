package com.careerpulse.analysis.entity;

import jakarta.persistence.*;

/**
 * 공고 요구사항 1건에 대한 비교 결과.
 *
 * matchedSkills / missingSkills / userEvidence / jobEvidence 는
 * AI 응답의 해당 부분을 JSON 문자열 그대로 저장한다.
 * (근거 원문을 가공하지 않고 보관하기 위한 것)
 */
@Entity
@Table(name = "analysis_items")
public class AnalysisItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AnalysisResult analysisResult;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @Column(name = "requirement", nullable = false, length = 500)
    private String requirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_category", nullable = false, length = 20)
    private RequirementCategory requirementCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 30)
    private MatchStatus matchStatus;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Lob
    @Column(name = "matched_skills", columnDefinition = "LONGTEXT")
    private String matchedSkills;

    @Lob
    @Column(name = "missing_skills", columnDefinition = "LONGTEXT")
    private String missingSkills;

    @Lob
    @Column(name = "user_evidence", columnDefinition = "LONGTEXT")
    private String userEvidence;

    @Lob
    @Column(name = "job_evidence", columnDefinition = "LONGTEXT")
    private String jobEvidence;

    protected AnalysisItem() {
    }

    public AnalysisItem(
            int itemOrder,
            String requirement,
            RequirementCategory requirementCategory,
            MatchStatus matchStatus,
            String reason,
            String matchedSkills,
            String missingSkills,
            String userEvidence,
            String jobEvidence
    ) {
        this.itemOrder = itemOrder;
        this.requirement = requirement;
        this.requirementCategory = requirementCategory;
        this.matchStatus = matchStatus;
        this.reason = reason;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.userEvidence = userEvidence;
        this.jobEvidence = jobEvidence;
    }

    void assignTo(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }

    public Long getItemId() {
        return itemId;
    }

    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public int getItemOrder() {
        return itemOrder;
    }

    public String getRequirement() {
        return requirement;
    }

    public RequirementCategory getRequirementCategory() {
        return requirementCategory;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public String getReason() {
        return reason;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public String getUserEvidence() {
        return userEvidence;
    }

    public String getJobEvidence() {
        return jobEvidence;
    }
}
