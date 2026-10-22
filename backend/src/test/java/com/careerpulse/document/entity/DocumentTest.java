package com.careerpulse.document.entity;

import com.careerpulse.user.entity.User;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTest {

    @Test
    void mapsDocumentAndChunkToDatabaseTables() {
        assertEquals(
                "user_documents",
                Document.class.getAnnotation(Table.class).name()
        );
        assertEquals(
                "document_chunks",
                DocumentChunk.class.getAnnotation(Table.class).name()
        );
    }

    @Test
    void createsDocumentWithPendingStatusAndManagesChunks() {
        Document document = document();
        DocumentChunk chunk = new DocumentChunk(0, "경험 근거", "page:1", null);

        document.addChunk(chunk);

        assertEquals(ProcessingStatus.PENDING, document.getProcessingStatus());
        assertNotNull(document.getUploadedAt());
        assertSame(document, chunk.getDocument());
        assertEquals(1, document.getChunks().size());

        document.clearChunks();

        assertTrue(document.getChunks().isEmpty());
        assertEquals(null, chunk.getDocument());
    }

    @Test
    void replacingFileClearsPreviousAnalysisAndChunks() {
        Document document = document();
        document.addChunk(new DocumentChunk(0, "기존 근거", "page:1", null));
        document.complete("기존 분석");

        document.replaceFile(
                "new.docx",
                "new-path.docx",
                FileFormat.DOCX,
                2048L
        );

        assertEquals("new.docx", document.getOriginalFilename());
        assertEquals(FileFormat.DOCX, document.getFileFormat());
        assertEquals(ProcessingStatus.PENDING, document.getProcessingStatus());
        assertEquals(null, document.getExtractedText());
        assertTrue(document.getChunks().isEmpty());
    }

    @Test
    void recordsProcessingFailureAndClearsItWhenRetryStarts() {
        Document document = document();

        document.markProcessing();
        assertEquals(ProcessingStatus.PROCESSING, document.getProcessingStatus());

        document.fail("문서 파싱 실패");
        assertEquals(ProcessingStatus.FAILED, document.getProcessingStatus());
        assertEquals("문서 파싱 실패", document.getProcessingError());
        assertNotNull(document.getAnalyzedAt());

        document.markProcessing();
        assertEquals(ProcessingStatus.PROCESSING, document.getProcessingStatus());
        assertEquals(null, document.getProcessingError());
    }

    private static Document document() {
        User user = new User(
                "owner@example.com",
                "테스트 사용자",
                "NEW",
                BigDecimal.ZERO,
                "v1"
        );
        return new Document(
                user,
                DocumentType.RESUME,
                "resume.pdf",
                "resume.pdf",
                FileFormat.PDF,
                1024L
        );
    }
}
