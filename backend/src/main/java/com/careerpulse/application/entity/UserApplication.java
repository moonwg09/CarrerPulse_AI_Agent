package com.careerpulse.application.entity;

import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_application",
                columnNames = {"user_id", "job_id"}
        )
)
public class UserApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "interview_date")
    private LocalDate interviewDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserApplication() {
    }

    public UserApplication(User user, JobPosting job) {
        this.user = user;
        this.job = job;
        this.status = ApplicationStatus.APPLIED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(ApplicationStatus status, LocalDate interviewDate) {
        this.status = status;
        this.interviewDate = status == ApplicationStatus.INTERVIEW ? interviewDate : null;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getApplicationId() { return applicationId; }
    public User getUser() { return user; }
    public JobPosting getJob() { return job; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDate getInterviewDate() { return interviewDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
