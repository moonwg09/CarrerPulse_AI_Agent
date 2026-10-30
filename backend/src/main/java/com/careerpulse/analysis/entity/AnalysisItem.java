package com.careerpulse.analysis.entity;

import jakarta.persistence.*;

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
    }

    public Long getItemId() { return itemId; }
    public AnalysisResult getAnalysisResult() { return analysisResult; }
    public JobRequirement getRequirement() { return requirement; }
    public Long getEvidenceChunkId() { return evidenceChunkId; }
    public Judgment getJudgment() { return judgment; }
    public String getAnalysisReason() { return analysisReason; }
    public boolean isUserConfirmed() { return userConfirmed; }
}
