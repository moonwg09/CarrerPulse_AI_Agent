package com.careerpulse.document.entity;

import com.careerpulse.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest {

    @Test
    void mapsEntitiesToErdTablesAndColumns() throws NoSuchFieldException {
        Table documentTable = Document.class.getAnnotation(Table.class);
        GeneratedValue documentIdGeneration = field(Document.class, "documentId")
                .getAnnotation(GeneratedValue.class);
        JoinColumn userJoinColumn = field(Document.class, "user")
                .getAnnotation(JoinColumn.class);

        assertThat(documentTable.name()).isEqualTo("user_documents");
        assertThat(documentIdGeneration.strategy()).isEqualTo(GenerationType.IDENTITY);
        assertThat(userJoinColumn.name()).isEqualTo("user_id");
        assertThat(userJoinColumn.nullable()).isFalse();

        Table chunkTable = DocumentChunk.class.getAnnotation(Table.class);
        GeneratedValue chunkIdGeneration = field(DocumentChunk.class, "chunkId")
                .getAnnotation(GeneratedValue.class);
        Column chunkTextColumn = field(DocumentChunk.class, "chunkText")
                .getAnnotation(Column.class);
        Column embeddingRefColumn = field(DocumentChunk.class, "embeddingRef")
                .getAnnotation(Column.class);

        assertThat(chunkTable.name()).isEqualTo("document_chunks");
        assertThat(chunkIdGeneration.strategy()).isEqualTo(GenerationType.IDENTITY);
        assertThat(chunkTextColumn.nullable()).isFalse();
        assertThat(embeddingRefColumn.length()).isEqualTo(1024);
    }

    @Test
    void createsDocumentWithPendingStatusAndUser() {
        User user = createUser();

        Document document = createDocument(user);

        assertThat(document.getUser()).isSameAs(user);
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.PENDING);
        assertThat(document.getUploadedAt()).isNotNull();
        assertThat(document.getChunks()).isEmpty();
    }

    @Test
    void changesProcessingStatus() {
        Document document = createDocument(createUser());

        document.markProcessing();
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSING);

        document.complete("extracted text");
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(document.getExtractedText()).isEqualTo("extracted text");
        assertThat(document.getAnalyzedAt()).isNotNull();

        document.fail();
        assertThat(document.getProcessingStatus()).isEqualTo(ProcessingStatus.FAILED);
    }

    @Test
    void addsAndRemovesChunkFromBothSides() {
        Document document = createDocument(createUser());
        DocumentChunk firstChunk = new DocumentChunk(0, "first", "page:1", null);
        DocumentChunk secondChunk = new DocumentChunk(1, "second", "page:2", "embedding:2");

        document.addChunk(firstChunk);
        document.addChunk(secondChunk);

        assertThat(document.getChunks()).containsExactly(firstChunk, secondChunk);
        assertThat(firstChunk.getDocument()).isSameAs(document);
        assertThat(secondChunk.getDocument()).isSameAs(document);

        document.removeChunk(firstChunk);

        assertThat(document.getChunks()).containsExactly(secondChunk);
        assertThat(firstChunk.getDocument()).isNull();

        document.clearChunks();

        assertThat(document.getChunks()).isEmpty();
        assertThat(secondChunk.getDocument()).isNull();
    }

    @Test
    void exposesUserConsentInformation() {
        User user = createUser();

        assertThat(user.isConsented()).isTrue();
        assertThat(user.getConsentVersion()).isEqualTo("DOCUMENT_ANALYSIS_V1");
        assertThat(user.getConsentedAt()).isNotNull();
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        return type.getDeclaredField(name);
    }

    private static User createUser() {
        return new User(
                "user@example.com",
                "Test User",
                "NEW",
                null,
                "DOCUMENT_ANALYSIS_V1"
        );
    }

    private static Document createDocument(User user) {
        return new Document(
                user,
                DocumentType.RESUME,
                "resume.pdf",
                "1/document-id.pdf",
                FileFormat.PDF,
                1024L
        );
    }
}
