package com.careerpulse.job.controller;

import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.job.dto.JobCollectionResponse;
import com.careerpulse.job.service.JobCollectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobCollectionService jobCollectionService;

    public JobController(JobCollectionService jobCollectionService) {
        this.jobCollectionService = jobCollectionService;
    }

    @PostMapping("/collect")
    public JobCollectionResponse collect(@Valid @RequestBody JobCollectionRequest request) {
        return jobCollectionService.collectAndStore(request);
    }
}
