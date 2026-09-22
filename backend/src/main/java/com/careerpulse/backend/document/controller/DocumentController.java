package com.careerpulse.backend.document.controller;

import com.careerpulse.backend.document.dto.DocumentResponse;
import com.careerpulse.backend.document.entity.DocumentType;
import com.careerpulse.backend.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/{documentType}")
    public DocumentResponse getDocument(@PathVariable DocumentType documentType) {
        Long userId = 1L; // TODO: 임시값 변경
        return documentService.getDocument(userId, documentType);
    }

    @DeleteMapping("/{documentType}")
    public ResponseEntity<Void> deleteDocument(@PathVariable DocumentType documentType) {
        Long userId = 1L;
        documentService.deleteDocument(userId, documentType);

        return ResponseEntity.noContent().build();
    }
}
