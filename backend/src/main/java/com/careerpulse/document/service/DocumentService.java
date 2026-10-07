package com.careerpulse.document.service;

import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.repository.DocumentRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(
            String email,
            DocumentType documentType
    ) {
        User user = findUserByEmail(email);
        Document document = findOwnedDocument(user.getUserId(), documentType);

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

    @Transactional
    public void deleteDocument(
            String email,
            DocumentType documentType
    ) {
        User user = findUserByEmail(email);
        Document document = findOwnedDocument(user.getUserId(), documentType);

        documentRepository.delete(document);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }

    private Document findOwnedDocument(
            Long userId,
            DocumentType documentType
    ) {
        return documentRepository
                .findByUser_UserIdAndDocumentType(userId, documentType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "문서를 찾을 수 없습니다."
                ));
    }
}
