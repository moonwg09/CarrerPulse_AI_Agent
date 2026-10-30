package com.careerpulse.document.service;

import com.careerpulse.document.client.DocumentRagClient;
import com.careerpulse.document.client.DocumentRagException;
import com.careerpulse.document.client.DocumentRagIndexResponse;
import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.document.entity.ProcessingStatus;
import com.careerpulse.document.repository.DocumentRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        MockMultipartFile file = docxFile("resume.docx");

        DocumentResponse response = service.store(
                EMAIL,
                DocumentType.RESUME,
                file
        );

        assertEquals(41L, response.documentId());
        assertEquals(DocumentType.RESUME, response.documentType());
        assertEquals(FileFormat.DOCX, response.fileFormat());
        assertEquals(ProcessingStatus.COMPLETED, response.processingStatus());
        assertNull(response.processingError());
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
                docxFile("new.docx")
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
    void getsRegisteredDocumentsOwnedByAuthenticatedUser() {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        User user = user(7L);
        Document resume = document(user, DocumentType.RESUME, "resume.pdf");
        Document portfolio = document(user, DocumentType.PORTFOLIO, "portfolio.pdf");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(documentRepository.findAllByUser_UserIdOrderByUploadedAtDesc(7L))
                .thenReturn(List.of(portfolio, resume));

        List<DocumentResponse> responses = service(userRepository, documentRepository)
                .getDocuments(EMAIL);

        assertEquals(2, responses.size());
        assertEquals(DocumentType.PORTFOLIO, responses.get(0).documentType());
        assertEquals(DocumentType.RESUME, responses.get(1).documentType());
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
    void keepsStoredDocumentAndFailureReasonWhenRagIndexingFails() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentRagClient ragClient = mock(DocumentRagClient.class);
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
        when(ragClient.index(
                any(), any(), any(), any(Boolean.class), any()
        )).thenThrow(new DocumentRagException("문서 색인 실패", new RuntimeException()));

        DocumentResponse response = service(
                userRepository,
                documentRepository,
                ragClient
        ).store(EMAIL, DocumentType.RESUME, docxFile("resume.docx"));

        assertEquals(ProcessingStatus.FAILED, response.processingStatus());
        assertEquals("문서 색인 실패", response.processingError());
        try (Stream<Path> storedFiles = Files.list(storageDirectory)) {
            assertEquals(1, storedFiles.count());
        }
    }

    @Test
    void preservesDocumentWhenRagDeletionFails() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentRagClient ragClient = mock(DocumentRagClient.class);
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
        ReflectionTestUtils.setField(document, "documentId", 41L);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(documentRepository.findByUser_UserIdAndDocumentType(
                7L,
                DocumentType.RESUME
        )).thenReturn(Optional.of(document));
        org.mockito.Mockito.doThrow(
                new DocumentRagException("검색 데이터 삭제 실패", new RuntimeException())
        ).when(ragClient).delete(7L, 41L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service(userRepository, documentRepository, ragClient)
                        .deleteDocument(EMAIL, DocumentType.RESUME)
        );

        assertEquals(502, exception.getStatusCode().value());
        verify(documentRepository, never()).delete(document);
        assertTrue(Files.exists(storedFile));
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
    void rejectsFileWhenExtensionAndActualFormatDoNotMatch() {
        DocumentService service = service(
                mock(UserRepository.class),
                mock(DocumentRepository.class)
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "not-a-pdf".getBytes()
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.store(EMAIL, DocumentType.RESUME, file)
        );

        assertEquals(400, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("실제 문서 형식"));
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
        DocumentRagClient ragClient = mock(DocumentRagClient.class);
        when(ragClient.index(
                any(), any(), any(), any(Boolean.class), any()
        )).thenReturn(new DocumentRagIndexResponse(
                41L,
                DocumentType.RESUME,
                1,
                "test",
                1,
                "추출된 문서"
        ));
        return service(userRepository, documentRepository, ragClient);
    }

    private DocumentService service(
            UserRepository userRepository,
            DocumentRepository documentRepository,
            DocumentRagClient ragClient
    ) {
        return new DocumentService(
                userRepository,
                documentRepository,
                ragClient,
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

    private static Document document(
            User user,
            DocumentType documentType,
            String filename
    ) {
        return new Document(
                user,
                documentType,
                filename,
                filename,
                FileFormat.PDF,
                1024L
        );
    }

    private static MockMultipartFile docxFile(String filename) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<document/>".getBytes());
            zip.closeEntry();
        }

        return new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                output.toByteArray()
        );
    }
}
