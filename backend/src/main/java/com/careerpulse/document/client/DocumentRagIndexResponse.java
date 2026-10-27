package com.careerpulse.document.client;

import com.careerpulse.document.entity.DocumentType;

public record DocumentRagIndexResponse(
        Long documentId,
        DocumentType documentType,
        int evidenceCount,
        String extractionMethod,
        int pageCount,
        String extractedText
) {
}
