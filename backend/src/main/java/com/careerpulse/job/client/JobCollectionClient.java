package com.careerpulse.job.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class JobCollectionClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public JobCollectionClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ai.server.url:http://127.0.0.1:8000}") String aiServerUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(aiServerUrl).build();
        this.objectMapper = objectMapper;
    }

    public AiJobCollectionResponse collect(AiJobCollectionRequest request) {
        try {
            AiJobCollectionResponse response = restClient.post()
                    .uri("/api/v1/jobs/collect")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.valueToTree(request).toString())
                    .retrieve()
                    .body(AiJobCollectionResponse.class);

            if (response == null || response.jobs() == null) {
                throw new JobCollectionClientException(
                        "채용공고 수집 응답이 비어 있습니다.",
                        null
                );
            }
            return response;
        } catch (JobCollectionClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new JobCollectionClientException(
                    failureMessage(exception),
                    exception
            );
        }
    }

    private String failureMessage(RestClientException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            String detail = responseException.getResponseBodyAsString();
            if (!detail.isBlank()) {
                return "FastAPI 채용공고 수집 요청에 실패했습니다. " + detail;
            }
        }
        return "FastAPI 채용공고 수집 요청에 실패했습니다.";
    }
}
