package com.careerpulse.user.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_job",
                        columnNames = {"user_id", "job_code"}
                )
        }

)
public class UserJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_job_id")
    private Long userJobId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "job_code", nullable = false, length = 50)
    private String jobCode;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    protected UserJob() {
    }

    public UserJob(
            User user,
            String jobCode,
            String jobName
    ) {
        this.user = user;
        this.jobCode = jobCode;
        this.jobName = jobName;
    }

    public Long getUserJobId() {
        return userJobId;
    }

    public User getUser() {
        return user;
    }

    public String getJobCode() {
        return jobCode;
    }

    public String getJobName() {
        return jobName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
