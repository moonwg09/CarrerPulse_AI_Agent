package com.careerpulse.document.client;

import com.careerpulse.document.entity.DocumentType;
import java.util.List;

public record DocumentRagIndexResponse(
        Long documentId,
        DocumentType documentType,
        int evidenceCount,
        String extractionMethod,
        int pageCount,
        String extractedText,
        List<Evidence> evidences
) {
    public record Evidence(
            String evidenceId,
            String text,
            String sourceLocation
    ) {}
}
