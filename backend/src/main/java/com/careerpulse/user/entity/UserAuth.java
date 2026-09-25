package com.careerpulse.user.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_auths")
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(
            name = "email_verified_at",
            insertable = false,
            updatable = false
    )
    private LocalDateTime emailVerifiedAt;

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

    protected UserAuth() {
    }

    public UserAuth(User user, String passwordHash) {
        this.user = user;
        this.provider = "LOCAL";
        this.passwordHash = passwordHash;
    }

    public Long getAuthId() {
        return authId;
    }
}
