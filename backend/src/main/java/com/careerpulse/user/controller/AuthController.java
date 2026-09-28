package com.careerpulse.user.controller;

import com.careerpulse.user.dto.LoginRequest;
import com.careerpulse.user.dto.LoginResponse;
import com.careerpulse.user.dto.SignupRequest;
import com.careerpulse.user.dto.SignupResponse;
import com.careerpulse.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {

        SignupResponse response =
                authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,

            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ){

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken
                        .unauthenticated(
                                request.email(),
                                request.password()
                        );

        Authentication authenticationResponse =
                authenticationManager.authenticate(
                        authenticationRequest
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authenticationResponse);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        var user = authService.findByEmail(
                authenticationResponse.getName()
        );

        return ResponseEntity.ok(
                new LoginResponse(
                        user.getUserId(),
                        user.getEmail(),
                        user.getName()
                )
        );
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {

        return Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        );
    }
}
