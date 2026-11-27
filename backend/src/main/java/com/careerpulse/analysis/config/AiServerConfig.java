package com.careerpulse.analysis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * FastAPI(AI 서버) 호출용 RestClient.
 *
 * 주소는 application.properties 의 ai.server.url 을 사용한다.
 * (기본값: http://127.0.0.1:8000)
 *
 * Gemini 호출이 포함된 Agent 파이프라인은 응답이 느릴 수 있으므로
 * 읽기 타임아웃을 길게 둔다. 필요하면 아래 속성으로 조정한다.
 *   ai.server.connect-timeout-seconds (기본 10)
 *   ai.server.read-timeout-seconds    (기본 300)
 */
@Configuration
public class AiServerConfig {

    @Bean
    public RestClient aiRestClient(
            @Value("${ai.server.url}") String aiServerUrl,

            @Value("${ai.server.connect-timeout-seconds:10}")
            long connectTimeoutSeconds,

            @Value("${ai.server.read-timeout-seconds:300}")
            long readTimeoutSeconds
    ) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(
                Duration.ofSeconds(connectTimeoutSeconds)
        );

        factory.setReadTimeout(
                Duration.ofSeconds(readTimeoutSeconds)
        );

        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(factory)
                .build();
    }
}
