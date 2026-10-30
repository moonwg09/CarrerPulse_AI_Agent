package com.careerpulse.job.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_postings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_external_job",
                columnNames = {"source", "external_job_id"}
        )
)
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "external_job_id", length = 100)
    private String externalJobId;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "job_category", length = 100)
    private String jobCategory;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "posted_date")
    private LocalDate postedDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "original_url", length = 1024)
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobPostingStatus status;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JobPosting() {
    }

    public JobPosting(String source, String externalJobId) {
        this.source = source;
        this.externalJobId = externalJobId;
        this.collectedAt = LocalDateTime.now();
        this.updatedAt = this.collectedAt;
        this.status = JobPostingStatus.UNKNOWN;
    }

    public void update(
            String companyName,
            String title,
            String jobCategory,
            String region,
            String description,
            LocalDate postedDate,
            LocalDate deadline,
            String originalUrl,
            JobPostingStatus status
    ) {
        this.companyName = companyName;
        this.title = title;
        this.jobCategory = jobCategory;
        this.region = region;
        this.description = description;
        this.postedDate = postedDate;
        this.deadline = deadline;
        this.originalUrl = originalUrl;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getJobId() {
        return jobId;
    }

    public String getSource() {
        return source;
    }

    public String getExternalJobId() {
        return externalJobId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getTitle() {
        return title;
    }

    public String getJobCategory() {
        return jobCategory;
    }

    public String getRegion() {
        return region;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public JobPostingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
