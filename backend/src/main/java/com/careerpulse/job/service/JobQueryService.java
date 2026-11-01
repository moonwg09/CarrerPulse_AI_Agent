package com.careerpulse.job.service;

import com.careerpulse.job.dto.JobPostingResponse;
import com.careerpulse.job.dto.JobPostingSummaryResponse;
import com.careerpulse.job.dto.JobRequirementResponse;
import com.careerpulse.analysis.repository.JobRequirementRepository;
import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.repository.JobPostingRepository;
import com.careerpulse.job.repository.JobPostingSpecifications;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.entity.UserJob;
import com.careerpulse.user.model.DevelopmentJobCatalog;
import com.careerpulse.user.repository.UserJobRepository;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobQueryService {

    private static final int MAX_RESULTS = 20;

    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final UserJobRepository userJobRepository;
    private final JobRequirementRepository jobRequirementRepository;

    public JobQueryService(
            JobPostingRepository jobPostingRepository,
            UserRepository userRepository,
            UserJobRepository userJobRepository,
            JobRequirementRepository jobRequirementRepository
    ) {
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
        this.userJobRepository = userJobRepository;
        this.jobRequirementRepository = jobRequirementRepository;
    }

    @Transactional(readOnly = true)
    public List<JobPostingSummaryResponse> search(
            String email,
            Long userJobId,
            String region,
            String keyword
    ) {
        User user = findUser(email);
        if (region == null || region.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "지역을 선택해야 합니다."
            );
        }
        UserJob interestJob = userJobRepository
                .findByUserJobIdAndUser_UserId(userJobId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "등록한 관심 직무를 선택해야 합니다."
                ));

        PageRequest pageRequest = PageRequest.of(
                0,
                MAX_RESULTS,
                Sort.by(
                        Sort.Order.desc("postedDate"),
                        Sort.Order.desc("jobId")
                )
        );
        return jobPostingRepository.findAll(
                        JobPostingSpecifications.search(
                                DevelopmentJobCatalog.fromCode(interestJob.getJobCode()).searchTerms(),
                                region,
                                keyword
                        ),
                        pageRequest
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobPostingResponse get(Long jobId) {
        return toResponse(jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "채용공고를 찾을 수 없습니다."
                )));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));
    }

    private JobPostingSummaryResponse toSummary(JobPosting posting) {
        return new JobPostingSummaryResponse(
                posting.getJobId(),
                posting.getCompanyName(),
                posting.getTitle(),
                posting.getJobCategory(),
                posting.getRegion(),
                posting.getPostedDate(),
                posting.getDeadline(),
                posting.getOriginalUrl(),
                posting.getStatus()
        );
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
                jobRequirementRepository
                        .findAllByJobIdOrderByRequirementId(posting.getJobId())
                        .stream()
                        .map(requirement -> new JobRequirementResponse(
                                requirement.getRequirementId(),
                                requirement.getRequirementType().name(),
                                requirement.getComparisonKind().name(),
                                requirement.getRequirementName(),
                                requirement.getDescription(),
                                requirement.getSourceQuote()
                        ))
                        .toList()
        );
    }
}
