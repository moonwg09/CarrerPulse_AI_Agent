package com.careerpulse.backend.document.service;

import com.careerpulse.backend.document.dto.DocumentResponse;
import com.careerpulse.backend.document.entity.Document;
import com.careerpulse.backend.document.entity.DocumentType;
import com.careerpulse.backend.document.repository.DocumentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    // TODO: final?
    private DocumentRepository documentRepository;

    // 조회
    public DocumentResponse getDocument(Long userId, DocumentType documentType) {
        Document document = documentRepository
                .findByUser_UserIdAndDocumentType(userId, documentType)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        return DocumentResponse.builder()
                .documentId(document.getDocumentId())
                .documentType(document.getDocumentType())
                .originalFilename(document.getOriginalFilename())
                .fileFormat(document.getFileFormat())
                .fileSize(document.getFileSize())
                .processingStatus(document.getProcessingStatus())
                .uploadedAt(document.getUploadedAt())
                .analyzedAt(document.getAnalyzedAt())
                .build();
    }

    // 삭제
    @Transactional
    public void deleteDocument(Long userId, DocumentType documentType) {
        Document document = documentRepository
                .findByUser_UserIdAndDocumentType(userId, documentType)
                .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다."));

        documentRepository.delete(document);
    }

    // TODO: 업로드 구현
}
