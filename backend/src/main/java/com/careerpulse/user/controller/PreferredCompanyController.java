package com.careerpulse.user.controller;

import com.careerpulse.user.dto.PreferredCompanyRequest;
import com.careerpulse.user.dto.PreferredCompanyResponse;
import com.careerpulse.user.service.PreferredCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/preferred-companies")
public class PreferredCompanyController {
    private final PreferredCompanyService service;

    public PreferredCompanyController(PreferredCompanyService service) {
        this.service = service;
    }

    @GetMapping
    public List<PreferredCompanyResponse> list(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @PostMapping
    public ResponseEntity<PreferredCompanyResponse> add(
            Authentication authentication,
            @Valid @RequestBody PreferredCompanyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.add(authentication.getName(), request));
    }

    @DeleteMapping("/{preferredCompanyId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long preferredCompanyId
    ) {
        service.delete(authentication.getName(), preferredCompanyId);
        return ResponseEntity.noContent().build();
    }
}
