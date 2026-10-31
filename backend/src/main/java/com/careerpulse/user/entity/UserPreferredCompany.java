package com.careerpulse.user.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_preferred_companies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_preferred_company",
                columnNames = {"user_id", "company_name"}
        )
)
public class UserPreferredCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preferred_company_id")
    private Long preferredCompanyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserPreferredCompany() {}

    public UserPreferredCompany(User user, String companyName) {
        this.user = user;
        this.companyName = companyName;
    }

    public Long getPreferredCompanyId() { return preferredCompanyId; }
    public String getCompanyName() { return companyName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
