package com.careerpulse.document.repository;

import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByUser_UserIdOrderByUploadedAtDesc(Long userId);

    Optional<Document> findByDocumentIdAndUser_UserId(
            Long documentId,
            Long userId
    );

    Optional<Document> findByUser_UserIdAndDocumentType(
            Long userId,
            DocumentType documentType
    );

    boolean existsByUser_UserIdAndDocumentType(
            Long userId,
            DocumentType documentType
    );
}
