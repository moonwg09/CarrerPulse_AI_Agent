package com.careerpulse.document.service;

import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.document.repository.DocumentRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private static final String EMAIL = "owner@example.com";

    @TempDir
    Path storageDirectory;

    @Test
    void storesFileAndReturnsDatabaseDocumentId() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        User user = user(7L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(documentRepository.findByUser_UserIdAndDocumentType(
                7L,
                DocumentType.RESUME
        )).thenReturn(Optional.empty());
        when(documentRepository.saveAndFlush(any(Document.class)))
                .thenAnswer(invocation -> {
                    Document document = invocation.getArgument(0);
                    ReflectionTestUtils.setField(document, "documentId", 41L);
                    return document;
                });

        DocumentService service = service(userRepository, documentRepository);
        MockMultipartFile file = file("resume.docx", "document-content");

        DocumentResponse response = service.store(
                EMAIL,
                DocumentType.RESUME,
                file
        );

        assertEquals(41L, response.documentId());
        assertEquals(DocumentType.RESUME, response.documentType());
        assertEquals(FileFormat.DOCX, response.fileFormat());
        try (Stream<Path> storedFiles = Files.list(storageDirectory)) {
            assertEquals(1, storedFiles.count());
        }
    }

    @Test
    void replacesExistingDocumentAndDeletesPreviousFile() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        User user = user(7L);
        Path previousFile = storageDirectory.resolve("previous.pdf");
        Files.writeString(previousFile, "previous");
        Document existing = new Document(
                user,
                DocumentType.RESUME,
                "old.pdf",
                previousFile.toString(),
                FileFormat.PDF,
                Files.size(previousFile)
        );
        ReflectionTestUtils.setField(existing, "documentId", 41L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(documentRepository.findByUser_UserIdAndDocumentType(
                7L,
                DocumentType.RESUME
        )).thenReturn(Optional.of(existing));
        when(documentRepository.saveAndFlush(existing)).thenReturn(existing);

        DocumentResponse response = service(userRepository, documentRepository).store(
                EMAIL,
                DocumentType.RESUME,
                file("new.docx", "new-content")
        );

        assertEquals(41L, response.documentId());
        assertEquals("new.docx", response.originalFilename());
        assertFalse(Files.exists(previousFile));
    }

    @Test
    void getsDocumentOwnedByAuthenticatedUser() {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        User user = user(7L);
        Document document = new Document(
                user,
                DocumentType.SELF_INTRO,
                "self-intro.pdf",
                storageDirectory.resolve("self-intro.pdf").toString(),
                FileFormat.PDF,
                1024L
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(documentRepository.findByUser_UserIdAndDocumentType(
                7L,
                DocumentType.SELF_INTRO
        )).thenReturn(Optional.of(document));

        DocumentResponse response = service(userRepository, documentRepository)
                .getDocument(EMAIL, DocumentType.SELF_INTRO);

        assertEquals(DocumentType.SELF_INTRO, response.documentType());
        assertEquals("self-intro.pdf", response.originalFilename());
    }

    @Test
    void deletesOwnedDocumentAndStoredFile() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        User user = user(7L);
        Path storedFile = storageDirectory.resolve("resume.pdf");
        Files.writeString(storedFile, "content");
        Document document = new Document(
                user,
                DocumentType.RESUME,
                "resume.pdf",
                storedFile.toString(),
                FileFormat.PDF,
                Files.size(storedFile)
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(documentRepository.findByUser_UserIdAndDocumentType(
                7L,
                DocumentType.RESUME
        )).thenReturn(Optional.of(document));

        service(userRepository, documentRepository)
                .deleteDocument(EMAIL, DocumentType.RESUME);

        verify(documentRepository).delete(document);
        verify(documentRepository).flush();
        assertFalse(Files.exists(storedFile));
    }

    @Test
    void rejectsUnsupportedFileBeforeDatabaseAccess() {
        DocumentService service = service(
                mock(UserRepository.class),
                mock(DocumentRepository.class)
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.exe",
                "application/octet-stream",
                "invalid".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.store(EMAIL, DocumentType.RESUME, file)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("PDF 또는 DOCX"));
    }

    @Test
    void rejectsAccessWhenOwnedDocumentDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        User user = user(7L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(documentRepository.findByUser_UserIdAndDocumentType(
                7L,
                DocumentType.RESUME
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service(userRepository, documentRepository)
                        .getDocument(EMAIL, DocumentType.RESUME)
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    private DocumentService service(
            UserRepository userRepository,
            DocumentRepository documentRepository
    ) {
        return new DocumentService(
                userRepository,
                documentRepository,
                storageDirectory.toString()
        );
    }

    private static User user(Long userId) {
        User user = new User(
                EMAIL,
                "테스트 사용자",
                "NEW",
                BigDecimal.ZERO,
                "v1"
        );
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private static MockMultipartFile file(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/octet-stream",
                content.getBytes()
        );
    }
}
