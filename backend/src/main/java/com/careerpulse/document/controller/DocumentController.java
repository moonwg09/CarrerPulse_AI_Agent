package com.careerpulse.document.controller;

import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadResume(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        return created(documentService.store(
                authentication.getName(),
                DocumentType.RESUME,
                file
        ));
    }

    @PostMapping(value = "/self-intro", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadSelfIntro(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        return created(documentService.store(
                authentication.getName(),
                DocumentType.SELF_INTRO,
                file
        ));
    }

    @GetMapping("/resume")
    public DocumentResponse getResume(Authentication authentication) {
        return documentService.getDocument(
                authentication.getName(),
                DocumentType.RESUME
        );
    }

    @GetMapping("/self-intro")
    public DocumentResponse getSelfIntro(Authentication authentication) {
        return documentService.getDocument(
                authentication.getName(),
                DocumentType.SELF_INTRO
        );
    }

    @DeleteMapping("/resume")
    public ResponseEntity<Void> deleteResume(Authentication authentication) {
        documentService.deleteDocument(
                authentication.getName(),
                DocumentType.RESUME
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/self-intro")
    public ResponseEntity<Void> deleteSelfIntro(Authentication authentication) {
        documentService.deleteDocument(
                authentication.getName(),
                DocumentType.SELF_INTRO
        );
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<DocumentResponse> created(DocumentResponse response) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
