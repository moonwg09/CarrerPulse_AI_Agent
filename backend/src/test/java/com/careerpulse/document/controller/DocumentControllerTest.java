package com.careerpulse.document.controller;

import com.careerpulse.common.config.SecurityConfig;
import com.careerpulse.document.dto.DocumentResponse;
import com.careerpulse.document.entity.DocumentType;
import com.careerpulse.document.service.DocumentService;
import com.careerpulse.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void passesAuthenticatedEmailToServiceWhenGettingDocument() throws Exception {
        when(documentService.getDocument(EMAIL, DocumentType.RESUME))
                .thenReturn(DocumentResponse.builder()
                        .documentId(10L)
                        .documentType(DocumentType.RESUME)
                        .build());

        mockMvc.perform(get("/api/documents/RESUME")
                        .with(user(EMAIL).roles("USER")))
                .andExpect(status().isOk());

        verify(documentService).getDocument(EMAIL, DocumentType.RESUME);
    }

    @Test
    void passesAuthenticatedEmailAndReturnsNoContentWhenDeletingDocument()
            throws Exception {
        mockMvc.perform(delete("/api/documents/RESUME")
                        .with(user(EMAIL).roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(EMAIL, DocumentType.RESUME);
    }

    @Test
    void blocksUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/documents/RESUME"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDeleteRequestWithoutCsrfToken() throws Exception {
        mockMvc.perform(delete("/api/documents/RESUME")
                        .with(user(EMAIL).roles("USER")))
                .andExpect(status().isForbidden());
    }
}
