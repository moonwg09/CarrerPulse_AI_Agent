package com.careerpulse.job.service;

import com.careerpulse.job.client.AiJobCollectionRequest;
import com.careerpulse.job.client.AiJobCollectionResponse;
import com.careerpulse.job.client.JobCollectionClient;
import com.careerpulse.job.client.JobCollectionClientException;
import com.careerpulse.job.dto.JobCollectionRequest;
import com.careerpulse.job.dto.JobCollectionResponse;
import com.careerpulse.job.dto.JobPostingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobCollectionService {

    private static final String NATIONWIDE_REGION = "전국";

    private final JobCollectionClient jobCollectionClient;
    private final JobPostingService jobPostingService;

    public JobCollectionService(
            JobCollectionClient jobCollectionClient,
            JobPostingService jobPostingService
    ) {
        this.jobCollectionClient = jobCollectionClient;
        this.jobPostingService = jobPostingService;
    }

    public JobCollectionResponse collectAndStore(JobCollectionRequest request) {
        AiJobCollectionResponse collection;
        try {
            collection = jobCollectionClient.collect(new AiJobCollectionRequest(
                    request.jobKeyword(),
                    NATIONWIDE_REGION,
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
}
