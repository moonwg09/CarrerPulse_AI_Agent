package com.careerpulse.user.repository;

import com.careerpulse.user.entity.UserPreferredCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferredCompanyRepository extends JpaRepository<UserPreferredCompany, Long> {
    long countByUser_UserId(Long userId);
    boolean existsByUser_UserIdAndCompanyNameIgnoreCase(Long userId, String companyName);
    List<UserPreferredCompany> findAllByUser_UserIdOrderByCreatedAtAsc(Long userId);
    Optional<UserPreferredCompany> findByPreferredCompanyIdAndUser_UserId(Long id, Long userId);
}
