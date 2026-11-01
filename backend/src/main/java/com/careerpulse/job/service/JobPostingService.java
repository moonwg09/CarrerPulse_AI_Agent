package com.careerpulse.job.service;

import com.careerpulse.job.client.AiJobCollectionResponse;
import com.careerpulse.job.client.AiRawJobPosting;
import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.entity.JobPostingStatus;
import com.careerpulse.job.repository.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class JobPostingService {

    private static final String SOFTWARE_CATEGORY = "소프트웨어";

    private final JobPostingRepository jobPostingRepository;
    private final Clock clock;

    @Autowired
    public JobPostingService(JobPostingRepository jobPostingRepository) {
        this(jobPostingRepository, Clock.systemDefaultZone());
    }

    JobPostingService(JobPostingRepository jobPostingRepository, Clock clock) {
        this.jobPostingRepository = jobPostingRepository;
        this.clock = clock;
    }

    @Transactional
    public List<JobPostingResponse> upsertAll(AiJobCollectionResponse collection) {
        List<JobPosting> postings = collection.jobs().stream()
                .map(this::upsert)
                .toList();
        jobPostingRepository.flush();
        return postings.stream().map(this::toResponse).toList();
    }

    private JobPosting upsert(AiRawJobPosting sourceJob) {
        validate(sourceJob);
        JobPosting posting = jobPostingRepository
                .findBySourceAndExternalJobId(
                        sourceJob.source(),
                        sourceJob.externalJobId()
                )
                .orElseGet(() -> new JobPosting(
                        sourceJob.source(),
                        sourceJob.externalJobId()
                ));

        posting.update(
                sourceJob.companyName(),
                sourceJob.title(),
                SOFTWARE_CATEGORY,
                sourceJob.region(),
                sourceJob.content(),
                sourceJob.postedDate(),
                sourceJob.closeDate(),
                sourceJob.url(),
                statusOf(sourceJob.closeDate())
        );
        return jobPostingRepository.save(posting);
    }

    private JobPostingStatus statusOf(LocalDate deadline) {
        if (deadline == null) {
            return JobPostingStatus.UNKNOWN;
        }
        return deadline.isBefore(LocalDate.now(clock))
                ? JobPostingStatus.CLOSED
                : JobPostingStatus.OPEN;
    }

    private void validate(AiRawJobPosting job) {
        if (isBlank(job.source()) || isBlank(job.externalJobId())) {
            throw badGateway("채용공고의 출처 또는 외부 공고 ID가 없습니다.");
        }
        if (isBlank(job.companyName()) || isBlank(job.title())) {
            throw badGateway("채용공고의 회사명 또는 제목이 없습니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseStatusException badGateway(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    private JobPostingResponse toResponse(JobPosting posting) {
        return new JobPostingResponse(
                posting.getJobId(),
                posting.getSource(),
                posting.getExternalJobId(),
                posting.getCompanyName(),
                posting.getTitle(),
                posting.getJobCategory(),
                posting.getRegion(),
                posting.getDescription(),
                posting.getPostedDate(),
                posting.getDeadline(),
                posting.getOriginalUrl(),
                posting.getStatus(),
                posting.getCollectedAt(),
                posting.getUpdatedAt(),
                java.util.List.of()
        );
    }
}
