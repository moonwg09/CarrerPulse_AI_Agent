package com.careerpulse.job.service;

import com.careerpulse.job.client.AiJobCollectionRequest;
import com.careerpulse.job.client.AiJobCollectionResponse;
import com.careerpulse.job.client.JobCollectionClient;
import com.careerpulse.job.client.JobCollectionClientException;
import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.job.dto.JobCollectionResponse;
import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.repository.JobPostingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.LinkedHashSet;

@Service
public class JobCollectionService {

    private final JobCollectionClient jobCollectionClient;
    private final JobPostingService jobPostingService;
    private final JobPostingRepository jobPostingRepository;

    public JobCollectionService(
            JobCollectionClient jobCollectionClient,
            JobPostingService jobPostingService,
            JobPostingRepository jobPostingRepository
    ) {
        this.jobCollectionClient = jobCollectionClient;
        this.jobPostingService = jobPostingService;
        this.jobPostingRepository = jobPostingRepository;
    }

    public JobCollectionResponse collectAndStore(JobCollectionRequest request) {
        AiJobCollectionResponse collection;
        try {
            collection = jobCollectionClient.collect(new AiJobCollectionRequest(
                    request.jobKeyword(),
                    request.region(),
                    request.limit()
            ));
        } catch (JobCollectionClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    exception.getMessage(),
                    exception
            );
        }

        List<JobPostingResponse> savedJobs = jobPostingService.upsertAll(collection);
        return new JobCollectionResponse(
                collection.jobKeyword(),
                collection.region(),
                collection.requestedLimit(),
                savedJobs.size(),
                savedJobs
        );
    }

    public List<String> searchCompanies(String query) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        jobPostingRepository
                .findTop10ByCompanyNameContainingIgnoreCaseOrderByUpdatedAtDesc(query)
                .stream()
                .map(job -> job.getCompanyName())
                .forEach(names::add);

        try {
            names.addAll(jobCollectionClient.searchCompanies(query));
        } catch (JobCollectionClientException exception) {
            if (names.isEmpty()) {
                throw exception;
            }
        }
        return names.stream().limit(10).toList();
    }
}
