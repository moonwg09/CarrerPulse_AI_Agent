package com.careerpulse.user.repository;

import com.careerpulse.user.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    // 특정 회원의 인증 방식 조회
    Optional<UserAuth> findByUser_UserIdAndProvider(
            Long userId,
            String provider
    );
}
