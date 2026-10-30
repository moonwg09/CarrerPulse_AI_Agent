package com.careerpulse.analysis.entity;

import com.careerpulse.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comparison_results")
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comparison_id")
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "matched_count", nullable = false)
    private int matchedCount;

    @Column(name = "comparable_count", nullable = false)
    private int comparableCount;

    @Column(name = "unknown_count", nullable = false)
    private int unknownCount;

    @Column(name = "match_rate", precision = 5, scale = 2)
    private BigDecimal matchRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private AnalysisStatus analysisStatus;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemId ASC")
    private List<AnalysisItem> items = new ArrayList<>();

    protected AnalysisResult() {
    }

    public AnalysisResult(User user, Long jobId) {
        this.user = user;
        this.jobId = jobId;
        resetPending();
    }

    public void resetPending() {
        matchedCount = 0;
        comparableCount = 0;
        unknownCount = 0;
        matchRate = null;
        analysisStatus = AnalysisStatus.PENDING;
        analyzedAt = null;
        items.clear();
    }

    public void complete(AnalysisScore score, List<AnalysisItem> newItems) {
        applyScore(score);
        analysisStatus = AnalysisStatus.COMPLETED;
        analyzedAt = LocalDateTime.now();
        items.clear();
        newItems.forEach(this::addItem);
    }

    public void refreshScore(AnalysisScore score) {
        applyScore(score);
        analysisStatus = AnalysisStatus.COMPLETED;
        analyzedAt = LocalDateTime.now();
    }

    private void applyScore(AnalysisScore score) {
        matchedCount = score.matchedCount();
        comparableCount = score.comparableCount();
        unknownCount = score.unknownCount();
        matchRate = score.overallMatchRate();
    }

    public void fail() {
        analysisStatus = AnalysisStatus.FAILED;
        analyzedAt = LocalDateTime.now();
        items.clear();
        matchedCount = 0;
        comparableCount = 0;
        unknownCount = 0;
        matchRate = null;
    }

    public void addItem(AnalysisItem item) {
        items.add(item);
        item.assignTo(this);
    }

    public Long getAnalysisId() { return analysisId; }
    public User getUser() { return user; }
    public Long getJobId() { return jobId; }
    public int getMatchedCount() { return matchedCount; }
    public int getComparableCount() { return comparableCount; }
    public int getUnknownCount() { return unknownCount; }
    public BigDecimal getMatchRate() { return matchRate; }
    public AnalysisStatus getAnalysisStatus() { return analysisStatus; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<AnalysisItem> getItems() { return items; }
}
