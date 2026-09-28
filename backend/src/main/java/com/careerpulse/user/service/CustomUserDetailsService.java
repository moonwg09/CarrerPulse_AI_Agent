package com.careerpulse.user.service;

import com.careerpulse.user.entity.User;
import com.careerpulse.user.entity.UserAuth;
import com.careerpulse.user.repository.UserAuthRepository;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            UserAuthRepository userAuthRepository
    ) {
        this.userRepository = userRepository;
        this.userAuthRepository = userAuthRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
        throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        UserAuth userAuth =
                userAuthRepository
                        .findByUser_UserIdAndProvider(
                                user.getUserId(),
                                "LOCAL"
                        )
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "일반 로그인 정보가 없습니다."
                                )
                        );

        if(!"ACTIVE".equals(user.getAccountStatus())) {
            throw new UsernameNotFoundException(
                    "사용할 수 없는 계정입니다."
            );
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(userAuth.getPasswordHash())
                .roles("USER")
                .build();
    }
}
