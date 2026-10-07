package com.careerpulse.document.service;

import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.Document;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.document.entity.ProcessingStatus;
import com.careerpulse.document.repository.DocumentRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final String EMAIL = "owner@example.com";
    private static final Long USER_ID = 1L;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void getsOwnedDocumentByLoginEmail() {
        User user = mock(User.class);
        Document document = mock(Document.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getUserId()).thenReturn(USER_ID);
        when(documentRepository.findByUser_UserIdAndDocumentType(
                USER_ID,
                DocumentType.RESUME
        )).thenReturn(Optional.of(document));
        when(document.getDocumentId()).thenReturn(10L);
        when(document.getDocumentType()).thenReturn(DocumentType.RESUME);
        when(document.getOriginalFilename()).thenReturn("resume.pdf");
        when(document.getFileFormat()).thenReturn(FileFormat.PDF);
        when(document.getFileSize()).thenReturn(1024L);
        when(document.getProcessingStatus()).thenReturn(ProcessingStatus.PENDING);

        DocumentResponse response = documentService.getDocument(
                EMAIL,
                DocumentType.RESUME
        );

        assertThat(response.getDocumentId()).isEqualTo(10L);
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.RESUME);
        assertThat(response.getOriginalFilename()).isEqualTo("resume.pdf");
        verify(userRepository).findByEmail(EMAIL);
        verify(documentRepository).findByUser_UserIdAndDocumentType(
                USER_ID,
                DocumentType.RESUME
        );
    }

    @Test
    void throwsNotFoundWhenOwnedDocumentDoesNotExist() {
        User user = mock(User.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getUserId()).thenReturn(USER_ID);
        when(documentRepository.findByUser_UserIdAndDocumentType(
                USER_ID,
                DocumentType.RESUME
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(
                EMAIL,
                DocumentType.RESUME
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((ResponseStatusException) exception).getStatusCode()
                ).isEqualTo(HttpStatus.NOT_FOUND));

        verify(documentRepository).findByUser_UserIdAndDocumentType(
                USER_ID,
                DocumentType.RESUME
        );
    }

    @Test
    void deletesOwnedDocumentByLoginEmail() {
        User user = mock(User.class);
        Document document = mock(Document.class);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(user.getUserId()).thenReturn(USER_ID);
        when(documentRepository.findByUser_UserIdAndDocumentType(
                USER_ID,
                DocumentType.RESUME
        )).thenReturn(Optional.of(document));

        documentService.deleteDocument(EMAIL, DocumentType.RESUME);

        verify(documentRepository).findByUser_UserIdAndDocumentType(
                USER_ID,
                DocumentType.RESUME
        );
        verify(documentRepository).delete(document);
    }

    @Test
    void throwsNotFoundWhenLoginUserDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(
                EMAIL,
                DocumentType.RESUME
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(
                        ((ResponseStatusException) exception).getStatusCode()
                ).isEqualTo(HttpStatus.NOT_FOUND));

        verifyNoInteractions(documentRepository);
    }
}
