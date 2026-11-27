package com.careerpulse.application.repository;

import com.careerpulse.application.entity.UserApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserApplicationRepository extends JpaRepository<UserApplication, Long> {
    List<UserApplication> findAllByUser_UserIdOrderByUpdatedAtDesc(Long userId);
    Optional<UserApplication> findByApplicationIdAndUser_UserId(Long applicationId, Long userId);
    Optional<UserApplication> findByUser_UserIdAndJob_JobId(Long userId, Long jobId);
}
