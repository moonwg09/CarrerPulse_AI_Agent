package com.careerpulse.document.dto;

import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.document.entity.ProcessingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentResponse {

    private Long documentId;
    private DocumentType documentType;
    private String originalFilename;
    private FileFormat fileFormat;
    private Long fileSize;
    private ProcessingStatus processingStatus;
    private LocalDateTime uploadedAt;
    private LocalDateTime analyzedAt;

    // TODO: 이력서, 자기소개서 조회 시 파일 정보만 vs 추출된 내용까지 조회
    // TODO: 내용까지 받는 경우 아래 추가
    // private String extractedText;
}
