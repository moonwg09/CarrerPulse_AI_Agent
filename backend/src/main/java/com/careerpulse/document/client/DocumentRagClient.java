package com.careerpulse.document.client;

import com.careerpulse.document.entity.DocumentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Path;

@Component
public class DocumentRagClient {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final RestClient restClient;

    @Autowired
    public DocumentRagClient(
            RestClient.Builder restClientBuilder,
            @Value("${ai.server.url:http://127.0.0.1:8000}") String aiServerUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(180000);
        this.restClient = restClientBuilder
                .baseUrl(aiServerUrl)
                .requestFactory(requestFactory)
                .build();
    }

    DocumentRagClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public DocumentRagIndexResponse index(
            Long userId,
            Long documentId,
            DocumentType documentType,
            boolean consented,
            Path storedPath
    ) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(storedPath));
        body.add("userId", userId.toString());
        body.add("documentType", documentType.name());
        body.add("consented", Boolean.toString(consented));

        try {
            DocumentRagIndexResponse response = restClient.put()
                    .uri("/api/v1/rag/documents/{documentId}", documentId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(DocumentRagIndexResponse.class);

            if (response == null
                    || !documentId.equals(response.documentId())
                    || documentType != response.documentType()) {
                throw new DocumentRagException(
                        "문서 색인 응답의 문서 정보가 요청과 일치하지 않습니다.",
                        null
                );
            }
            return response;
        } catch (RestClientException exception) {
            throw failure("문서 색인", exception);
        }
    }

    public void delete(Long userId, Long documentId) {
        try {
            restClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/rag/documents/{documentId}")
                            .queryParam("userId", userId)
                            .build(documentId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw failure("문서 검색 데이터 삭제", exception);
        }
    }

    private DocumentRagException failure(
            String operation,
            RestClientException exception
    ) {
        String detail = exception.getMessage();
        if (exception instanceof RestClientResponseException responseException) {
            String responseBody = responseException.getResponseBodyAsString();
            if (!responseBody.isBlank()) {
                detail = responseBody;
            }
        }

        String message = operation + "에 실패했습니다.";
        if (detail != null && !detail.isBlank()) {
            message += " " + detail;
        }
        if (message.length() > MAX_ERROR_LENGTH) {
            message = message.substring(0, MAX_ERROR_LENGTH);
        }

        return new DocumentRagException(message, exception);
    }
}
