package com.careerpulse.document.entity;

import com.careerpulse.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_documents")
@Getter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false, length = 10)
    private FileFormat fileFormat;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DocumentChunk> chunks = new ArrayList<>();

    protected Document() {
    }

    public Document(
            User user,
            DocumentType documentType,
            String originalFilename,
            String storagePath,
            FileFormat fileFormat,
            Long fileSize
    ) {
        this.user = user;
        this.documentType = documentType;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.fileFormat = fileFormat;
        this.fileSize = fileSize;
        this.processingStatus = ProcessingStatus.PENDING;
        this.uploadedAt = LocalDateTime.now();
    }

    public void markProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
    }

    public void complete(String extractedText) {
        this.extractedText = extractedText;
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.analyzedAt = LocalDateTime.now();
    }

    public void fail() {
        this.processingStatus = ProcessingStatus.FAILED;
    }

    public void addChunk(DocumentChunk chunk) {
        chunks.add(chunk);
        chunk.assignDocument(this);
    }

    public void removeChunk(DocumentChunk chunk) {
        if (chunks.remove(chunk)) {
            chunk.assignDocument(null);
        }
    }

    public void clearChunks() {
        chunks.forEach(chunk -> chunk.assignDocument(null));
        chunks.clear();
    }
}
