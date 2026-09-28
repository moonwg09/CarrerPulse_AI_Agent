package com.careerpulse.user.controller;

import com.careerpulse.user.dto.UpdateUserRequest;
import com.careerpulse.user.dto.UserJobRequest;
import com.careerpulse.user.dto.UserJobResponse;
import com.careerpulse.user.dto.UserResponse;
import com.careerpulse.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ){
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            Authentication authentication
    ){
        String email = authentication.getName();

        UserResponse response =
                userService.getMyInfo(email);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyInfo(

            Authentication authentication,

            @Valid
            @RequestBody
            UpdateUserRequest request
    ) {

        String email = authentication.getName();

        UserResponse response =
                userService.updateMyInfo(
                        email,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/jobs")
    public ResponseEntity<UserJobResponse> addMyJob(
            Authentication authentication,
            @Valid @RequestBody UserJobRequest request
    ) {

        String email = authentication.getName();

        UserJobResponse response =
                userService.addUserJob(
                        email,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/me/jobs")
    public ResponseEntity<List<UserJobResponse>> getMyJobs(
            Authentication authentication
    ) {

        String email = authentication.getName();

        List<UserJobResponse> response =
                userService.getMyJobs(email);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/jobs/{userJobId}")
    public ResponseEntity<Void> deleteMyJob(
            Authentication authentication,
            @PathVariable Long userJobId
    ) {

        String email = authentication.getName();

        userService.deleteMyJob(
                email,
                userJobId
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            Authentication authentication,
            HttpServletRequest request
    ) {

        String email = authentication.getName();

        userService.withdraw(email);

        request.getSession(false).invalidate();

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }
}
