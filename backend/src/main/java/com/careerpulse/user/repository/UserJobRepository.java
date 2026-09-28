package com.careerpulse.user.repository;

import com.careerpulse.user.entity.UserJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJobRepository
extends JpaRepository<UserJob, Long> {

    // 특정 사용자가 특정 직무를 이미 등록했는지 확인
    boolean existsByUser_UserIdAndJobCode(
            Long userId,
            String jobCode
    );

    // 특정 사용자의 관심 직무 전체 조회
    List<UserJob> findAllByUser_UserId(
            Long userId
    );

    // 현재 로그인 사용자의 특정 관심 직무 조회
    Optional<UserJob> findByUserJobIdAndUser_UserId(
            Long userJobId,
            Long userID
    );
}
