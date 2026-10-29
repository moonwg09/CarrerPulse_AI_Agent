package com.careerpulse.job.controller;

import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.job.dto.JobCollectionResponse;
import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.dto.JobPostingSummaryResponse;
import com.careerpulse.job.service.JobCollectionService;
import com.careerpulse.job.service.JobQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobCollectionService jobCollectionService;
    private final JobQueryService jobQueryService;

    public JobController(
            JobCollectionService jobCollectionService,
            JobQueryService jobQueryService
    ) {
        this.jobCollectionService = jobCollectionService;
        this.jobQueryService = jobQueryService;
    }

    @PostMapping("/collect")
    public JobCollectionResponse collect(@Valid @RequestBody JobCollectionRequest request) {
        return jobCollectionService.collectAndStore(request);
    }

    @GetMapping
    public List<JobPostingSummaryResponse> search(
            Authentication authentication,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword
    ) {
        return jobQueryService.search(
                authentication.getName(),
                region,
                keyword
        );
    }

    @GetMapping("/{jobId}")
    public JobPostingResponse get(@PathVariable Long jobId) {
        return jobQueryService.get(jobId);
    }
}
