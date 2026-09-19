package com.careerpulse.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class PythonTestController {

    private final RestClient restClient =
            RestClient.create("http://127.0.0.1:8000");

    @GetMapping("/api/python/test")
    public FastApiResponse testPython() {
        return restClient.get()
                .uri("/")
                .retrieve()
                .body(FastApiResponse.class);
    }

    public record FastApiResponse(String message){

    }
}
