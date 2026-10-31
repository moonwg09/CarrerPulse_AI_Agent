package com.careerpulse.analysis.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "comparison_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comparison_requirement",
                columnNames = {"comparison_id", "requirement_id"}
        )
)
public class AnalysisItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comparison_id", nullable = false)
    private AnalysisResult analysisResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private JobRequirement requirement;

    @Column(name = "evidence_chunk_id")
    private Long evidenceChunkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "judgment", nullable = false, length = 30)
    private Judgment judgment;

    @Column(name = "analysis_reason", columnDefinition = "TEXT")
    private String analysisReason;

    @Column(name = "user_confirmed", nullable = false)
    private boolean userConfirmed;

    @Column(name = "user_response_type", length = 30)
    private String userResponseType;

    @Column(name = "user_experience_text", columnDefinition = "TEXT")
    private String userExperienceText;

    @Column(name = "rejudged_at")
    private LocalDateTime rejudgedAt;

    protected AnalysisItem() {
    }

    public AnalysisItem(
            JobRequirement requirement,
            Long evidenceChunkId,
            Judgment judgment,
            String analysisReason,
            boolean userConfirmed
    ) {
        this.requirement = requirement;
        this.evidenceChunkId = evidenceChunkId;
        this.judgment = judgment;
        this.analysisReason = analysisReason;
        this.userConfirmed = userConfirmed;
    }

    void assignTo(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }

    public void confirmNoExperience() {
        this.judgment = Judgment.NO_EXPERIENCE;
        this.userConfirmed = true;
        this.userResponseType = Judgment.NO_EXPERIENCE.name();
        this.userExperienceText = null;
        this.rejudgedAt = LocalDateTime.now();
    }

    public void applyUserRejudgment(
            String responseType,
            String experienceText,
            Judgment judgment,
            String reason,
            Long evidenceChunkId
    ) {
        if (judgment == Judgment.NO_EXPERIENCE) {
            throw new IllegalArgumentException("경험 없음은 별도 확인 절차를 사용해야 합니다.");
        }
        this.userResponseType = responseType;
        this.userExperienceText = experienceText;
        this.judgment = judgment;
        this.analysisReason = reason;
        this.evidenceChunkId = evidenceChunkId;
        this.userConfirmed = true;
        this.rejudgedAt = LocalDateTime.now();
    }

    public Long getItemId() { return itemId; }
    public AnalysisResult getAnalysisResult() { return analysisResult; }
    public JobRequirement getRequirement() { return requirement; }
    public Long getEvidenceChunkId() { return evidenceChunkId; }
    public Judgment getJudgment() { return judgment; }
    public String getAnalysisReason() { return analysisReason; }
    public boolean isUserConfirmed() { return userConfirmed; }
    public String getUserResponseType() { return userResponseType; }
    public String getUserExperienceText() { return userExperienceText; }
    public LocalDateTime getRejudgedAt() { return rejudgedAt; }
}
