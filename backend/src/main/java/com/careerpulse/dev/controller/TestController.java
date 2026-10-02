package com.careerpulse.dev.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public Map<String, String> test() {

        return Map.of(
                "message",
                "React와 Spring Boot 연결 성공!"
        );
    }
}