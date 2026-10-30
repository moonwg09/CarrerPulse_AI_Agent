package com.careerpulse.job.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JobCollectionClient {

    private final RestClient restClient;

    public JobCollectionClient(
            RestClient.Builder restClientBuilder,
            @Value("${ai.server.url:http://127.0.0.1:8000}") String aiServerUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(aiServerUrl).build();
    }

    public AiJobCollectionResponse collect(AiJobCollectionRequest request) {
        try {
            AiJobCollectionResponse response = restClient.post()
                    .uri("/api/v1/jobs/collect")
                    .body(request)
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
                    "FastAPI 채용공고 수집 요청에 실패했습니다.",
                    exception
            );
        }
    }
}
