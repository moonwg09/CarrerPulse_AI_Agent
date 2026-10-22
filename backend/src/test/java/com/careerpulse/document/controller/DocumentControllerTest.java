package com.careerpulse.document.controller;

import com.careerpulse.common.config.SecurityConfig;
import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.entity.FileFormat;
import com.careerpulse.document.entity.ProcessingStatus;
import com.careerpulse.document.service.DocumentService;
import com.careerpulse.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerTest {

    private static final String EMAIL = "owner@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void uploadsResumeForAuthenticatedUser() throws Exception {
        MockMultipartFile file = file("resume.pdf");
        when(documentService.store(eq(EMAIL), eq(DocumentType.RESUME), any()))
                .thenReturn(response(DocumentType.RESUME));

        mockMvc.perform(multipart("/api/v1/documents/resume")
                        .file(file)
                        .with(user(EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(documentService).store(eq(EMAIL), eq(DocumentType.RESUME), any());
    }

    @Test
    void uploadsSelfIntroForAuthenticatedUser() throws Exception {
        MockMultipartFile file = file("self-intro.pdf");
        when(documentService.store(eq(EMAIL), eq(DocumentType.SELF_INTRO), any()))
                .thenReturn(response(DocumentType.SELF_INTRO));

        mockMvc.perform(multipart("/api/v1/documents/self-intro")
                        .file(file)
                        .with(user(EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(documentService).store(eq(EMAIL), eq(DocumentType.SELF_INTRO), any());
    }

    @Test
    void uploadsPortfolioForAuthenticatedUser() throws Exception {
        MockMultipartFile file = file("portfolio.pdf");
        when(documentService.store(eq(EMAIL), eq(DocumentType.PORTFOLIO), any()))
                .thenReturn(response(DocumentType.PORTFOLIO));

        mockMvc.perform(multipart("/api/v1/documents/portfolio")
                        .file(file)
                        .with(user(EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(documentService).store(eq(EMAIL), eq(DocumentType.PORTFOLIO), any());
    }

    @Test
    void getsRegisteredDocumentsForAuthenticatedUser() throws Exception {
        when(documentService.getDocuments(EMAIL))
                .thenReturn(List.of(response(DocumentType.RESUME)));

        mockMvc.perform(get("/api/v1/documents")
                        .with(user(EMAIL).roles("USER")))
                .andExpect(status().isOk());

        verify(documentService).getDocuments(EMAIL);
    }

    @Test
    void getsResumeForAuthenticatedUser() throws Exception {
        when(documentService.getDocument(EMAIL, DocumentType.RESUME))
                .thenReturn(response(DocumentType.RESUME));

        mockMvc.perform(get("/api/v1/documents/resume")
                        .with(user(EMAIL).roles("USER")))
                .andExpect(status().isOk());

        verify(documentService).getDocument(EMAIL, DocumentType.RESUME);
    }

    @Test
    void getsSelfIntroForAuthenticatedUser() throws Exception {
        when(documentService.getDocument(EMAIL, DocumentType.SELF_INTRO))
                .thenReturn(response(DocumentType.SELF_INTRO));

        mockMvc.perform(get("/api/v1/documents/self-intro")
                        .with(user(EMAIL).roles("USER")))
                .andExpect(status().isOk());

        verify(documentService).getDocument(EMAIL, DocumentType.SELF_INTRO);
    }

    @Test
    void getsPortfolioForAuthenticatedUser() throws Exception {
        when(documentService.getDocument(EMAIL, DocumentType.PORTFOLIO))
                .thenReturn(response(DocumentType.PORTFOLIO));

        mockMvc.perform(get("/api/v1/documents/portfolio")
                        .with(user(EMAIL).roles("USER")))
                .andExpect(status().isOk());

        verify(documentService).getDocument(EMAIL, DocumentType.PORTFOLIO);
    }

    @Test
    void deletesResumeForAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/resume")
                        .with(user(EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(EMAIL, DocumentType.RESUME);
    }

    @Test
    void deletesSelfIntroForAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/self-intro")
                        .with(user(EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(EMAIL, DocumentType.SELF_INTRO);
    }

    @Test
    void deletesPortfolioForAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/portfolio")
                        .with(user(EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(EMAIL, DocumentType.PORTFOLIO);
    }

    @Test
    void blocksUnauthenticatedDocumentRequest() throws Exception {
        mockMvc.perform(get("/api/v1/documents/resume"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDeleteWithoutCsrfToken() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/resume")
                        .with(user(EMAIL).roles("USER")))
                .andExpect(status().isForbidden());
    }

    private static MockMultipartFile file(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/pdf",
                "content".getBytes()
        );
    }

    private static DocumentResponse response(DocumentType documentType) {
        return new DocumentResponse(
                10L,
                documentType,
                documentType.name().toLowerCase() + ".pdf",
                FileFormat.PDF,
                1024L,
                ProcessingStatus.PENDING,
                null,
                null,
                null
        );
    }
}
