package com.careerpulse.application.controller;

import com.careerpulse.application.dto.*;
import com.careerpulse.application.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public List<ApplicationResponse> getMine(Authentication authentication) {
        return applicationService.getMine(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(
            Authentication authentication,
            @Valid @RequestBody ApplicationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.create(authentication.getName(), request));
    }

    @PatchMapping("/{applicationId}")
    public ApplicationResponse update(
            Authentication authentication,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationUpdateRequest request
    ) {
        return applicationService.update(authentication.getName(), applicationId, request);
    }
}
