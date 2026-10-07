package com.careerpulse.document.controller;

import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/{documentType}")
    public DocumentResponse getDocument(
            Authentication authentication,
            @PathVariable DocumentType documentType
    ) {
        String email = authentication.getName();

        return documentService.getDocument(email, documentType);
    }

    @DeleteMapping("/{documentType}")
    public ResponseEntity<Void> deleteDocument(
            Authentication authentication,
            @PathVariable DocumentType documentType
    ) {
        String email = authentication.getName();

        documentService.deleteDocument(email, documentType);

        return ResponseEntity.noContent().build();
    }
}
