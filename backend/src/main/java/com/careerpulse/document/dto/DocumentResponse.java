package com.careerpulse.document.dto;

import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.document.entity.ProcessingStatus;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long documentId,
        DocumentType documentType,
        String originalFilename,
        FileFormat fileFormat,
        Long fileSize,
        ProcessingStatus processingStatus,
        LocalDateTime uploadedAt,
        LocalDateTime analyzedAt
) {
}
