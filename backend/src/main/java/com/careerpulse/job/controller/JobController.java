package com.careerpulse.job.controller;

import com.careerpulse.job.client.JobCollectionClientException;
import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.job.dto.JobCollectionResponse;
import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.dto.JobPostingSummaryResponse;
import com.careerpulse.job.service.JobCollectionService;
import com.careerpulse.job.service.JobQueryService;
import com.careerpulse.job.service.JobBookmarkService;
import com.careerpulse.job.dto.JobBookmarkResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobCollectionService jobCollectionService;
    private final JobQueryService jobQueryService;
    private final JobBookmarkService jobBookmarkService;

    public JobController(
            JobCollectionService jobCollectionService,
            JobQueryService jobQueryService,
            JobBookmarkService jobBookmarkService
    ) {
        this.jobCollectionService = jobCollectionService;
        this.jobQueryService = jobQueryService;
        this.jobBookmarkService = jobBookmarkService;
    }

    @PostMapping("/collect")
    public JobCollectionResponse collect(@Valid @RequestBody JobCollectionRequest request) {
        return jobCollectionService.collectAndStore(request);
    }

    @GetMapping
    public List<JobPostingSummaryResponse> search(
            Authentication authentication,
            @RequestParam Long userJobId,
            @RequestParam String region,
            @RequestParam(required = false) String keyword
    ) {
        return jobQueryService.search(
                authentication.getName(),
                userJobId,
                region,
                keyword
        );
    }

    @GetMapping("/companies")
    public List<String> companies(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "기업명은 두 글자 이상 입력해 주세요."
            );
        }
        try {
            return jobCollectionService.searchCompanies(query.trim());
        } catch (JobCollectionClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    exception.getMessage(),
                    exception
            );
        }
    }

    @GetMapping("/{jobId}")
    public JobPostingResponse get(@PathVariable Long jobId) {
        return jobQueryService.get(jobId);
    }

    @GetMapping("/bookmarks")
    public List<JobPostingSummaryResponse> bookmarks(Authentication authentication) {
        return jobBookmarkService.list(authentication.getName());
    }

    @GetMapping("/{jobId}/bookmark")
    public JobBookmarkResponse bookmarkStatus(Authentication authentication, @PathVariable Long jobId) {
        return jobBookmarkService.status(authentication.getName(), jobId);
    }

    @PostMapping("/{jobId}/bookmark")
    public JobBookmarkResponse addBookmark(Authentication authentication, @PathVariable Long jobId) {
        return jobBookmarkService.add(authentication.getName(), jobId);
    }

    @DeleteMapping("/{jobId}/bookmark")
    public void deleteBookmark(Authentication authentication, @PathVariable Long jobId) {
        jobBookmarkService.delete(authentication.getName(), jobId);
    }
}
