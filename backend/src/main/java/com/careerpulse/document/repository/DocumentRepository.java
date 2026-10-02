package com.careerpulse.document.repository;

import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByUser_UserIdAndDocumentType(
            Long userId,
            DocumentType documentType
    );
}
