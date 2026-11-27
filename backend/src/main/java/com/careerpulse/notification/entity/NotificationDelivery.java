package com.careerpulse.notification.entity;

import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_delivery",
                columnNames = {"user_id", "job_id", "notification_type"}
        )
)
public class NotificationDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Column(name = "sent_at", insertable = false, updatable = false)
    private LocalDateTime sentAt;

    protected NotificationDelivery() {}

    public NotificationDelivery(User user, JobPosting job, NotificationType notificationType) {
        this.user = user;
        this.job = job;
        this.notificationType = notificationType;
    }
}
