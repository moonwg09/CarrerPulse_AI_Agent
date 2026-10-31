package com.careerpulse.job.entity;

import com.careerpulse.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_job_bookmark",
                columnNames = {"user_id", "job_id"}
        )
)
public class JobBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected JobBookmark() {}

    public JobBookmark(User user, JobPosting job) {
        this.user = user;
        this.job = job;
    }

    public Long getBookmarkId() { return bookmarkId; }
    public JobPosting getJob() { return job; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
