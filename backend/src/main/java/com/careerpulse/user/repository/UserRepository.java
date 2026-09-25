package com.careerpulse.user.repository;

import com.careerpulse.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일 중복 확인
    boolean existsByEmail(String email);

    // 이메일로 회원 조회
    Optional<User> findByEmail(String email);
}
