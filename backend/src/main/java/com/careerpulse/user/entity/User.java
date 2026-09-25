package com.careerpulse.user.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "career_type", nullable = false)
    private String careerType;

    @Column(name = "career_years")
    private BigDecimal careerYears;

    @Column(name ="consented", nullable = false)
    private boolean consented;

    @Column(name = "consent_version")
    private String consentVersion;

    @Column(name = "consented_at")
    private LocalDateTime consentedAt;

    @Column(name = "account_status", nullable = false)
    private String accountStatus = "ACTIVE";

    @Column(
            name = "updated_at",
            insertable = false,
            updatable = false
    )

    private LocalDateTime updatedAt;

    protected User() {

    }

    public User(
            String email,
            String name,
            String careerType,
            BigDecimal careerYears,
            String consentVersion
    ) {
        this.email = email;
        this.name = name;
        this.careerType = careerType;
        this.careerYears = careerYears;
        this.consented = true;
        this.consentVersion = consentVersion;
        this.consentedAt = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }




}
