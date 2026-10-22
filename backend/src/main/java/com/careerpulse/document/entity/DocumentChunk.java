package com.careerpulse.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_chunk",
                columnNames = {"document_id", "chunk_index"}
        )
)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "source_location", length = 255)
    private String sourceLocation;

    @Column(name = "embedding_ref", length = 1024)
    private String embeddingRef;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected DocumentChunk() {
    }

    public DocumentChunk(
            int chunkIndex,
            String chunkText,
            String sourceLocation,
            String embeddingRef
    ) {
        this.chunkIndex = chunkIndex;
        this.chunkText = chunkText;
        this.sourceLocation = sourceLocation;
        this.embeddingRef = embeddingRef;
        this.createdAt = LocalDateTime.now();
    }

    void assignDocument(Document document) {
        this.document = document;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public Document getDocument() {
        return document;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getChunkText() {
        return chunkText;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public String getEmbeddingRef() {
        return embeddingRef;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
