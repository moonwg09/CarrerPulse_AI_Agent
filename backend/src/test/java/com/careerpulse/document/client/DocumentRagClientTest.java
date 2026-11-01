package com.careerpulse.document.client;

import com.careerpulse.document.entity.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DocumentRagClientTest {

    @TempDir
    Path tempDirectory;

    private MockRestServiceServer server;
    private DocumentRagClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://127.0.0.1:8000");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DocumentRagClient(builder.build());
    }

    @Test
    void sendsDocumentIndexRequestUsingConfirmedContract() throws Exception {
        Path file = tempDirectory.resolve("resume.pdf");
        Files.writeString(file, "%PDF-1.4 test");

        server.expect(requestTo("http://127.0.0.1:8000/api/v1/rag/documents/41"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(
                        """
                        {
                          "documentId": 41,
                          "documentType": "RESUME",
                          "evidenceCount": 2,
                          "extractionMethod": "pymupdf",
                          "pageCount": 1,
                          "extractedText": "FastAPI 개발 경험"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        DocumentRagIndexResponse response = client.index(
                7L,
                41L,
                DocumentType.RESUME,
                true,
                file
        );

        assertEquals(41L, response.documentId());
        assertEquals(2, response.evidenceCount());
        assertEquals("FastAPI 개발 경험", response.extractedText());
        server.verify();
    }

    @Test
    void sendsDocumentDeleteRequestUsingOwnerId() {
        server.expect(requestTo(
                        "http://127.0.0.1:8000/api/v1/rag/documents/41?userId=7"
                ))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(
                        "{\"documentId\":41,\"deletedEvidenceCount\":2}",
                        MediaType.APPLICATION_JSON
                ));

        client.delete(7L, 41L);

        server.verify();
    }

    @Test
    void exposesFastApiFailureReason() throws Exception {
        Path file = tempDirectory.resolve("resume.pdf");
        Files.writeString(file, "%PDF-1.4 test");

        server.expect(requestTo("http://127.0.0.1:8000/api/v1/rag/documents/41"))
                .andRespond(withServerError()
                        .body("{\"detail\":\"임베딩 모델을 불러오지 못했습니다.\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        DocumentRagException exception = assertThrows(
                DocumentRagException.class,
                () -> client.index(
                        7L,
                        41L,
                        DocumentType.RESUME,
                        true,
                        file
                )
        );

        assertTrue(exception.getMessage().contains("임베딩 모델"));
        server.verify();
    }
}
