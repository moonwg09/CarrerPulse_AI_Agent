package com.careerpulse.user.service;

import com.careerpulse.user.dto.SignupRequest;
import com.careerpulse.user.dto.SignupResponse;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.entity.UserAuth;
import com.careerpulse.user.repository.UserAuthRepository;
import com.careerpulse.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            UserAuthRepository userAuthRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {

        // 이메일 형식 정리
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 이메일 중복 검사
        if(userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 이메일입니다."
            );
        }

        // 경력자 경력연수 검사
        if ("EXPERIENCED".equals(request.careerType())
                && request.careerYears() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "경력자는 경력연수를 입력해야 합니다."
            );
        }

        // 개인정보 동의 검사
        if (!Boolean.TRUE.equals(request.consented())
                || request.consentVersion() == null
                || request.consentVersion().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "개인정보 도으이 정보가 올바르지 않습니다."
            );
        }

        // user 객체 생성
        User user = new User(
                email,
                request.name(),
                request.careerType(),
                request.careerYears(),
                request.consentVersion()
        );

        // users 테이블 저장
        User savedUser = userRepository.save(user);

        // 비밀번호 해시 생성
        String encodedPassword = passwordEncoder.encode(
                request.password()
        );

        // userAuth 객체 생성
        UserAuth userAuth = new UserAuth(
                savedUser,
                encodedPassword
        );

        // user_auths 테이블 저장
        userAuthRepository.save(userAuth);

        return new SignupResponse(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getName()
        );
    }
}
