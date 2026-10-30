package com.careerpulse.application.service;

import com.careerpulse.application.dto.*;
import com.careerpulse.application.entity.ApplicationStatus;
import com.careerpulse.application.entity.UserApplication;
import com.careerpulse.application.repository.UserApplicationRepository;
import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.repository.JobPostingRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ApplicationService {
    private final UserApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobPostingRepository jobRepository;

    public ApplicationService(
            UserApplicationRepository applicationRepository,
            UserRepository userRepository,
            JobPostingRepository jobRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
    }

    public List<ApplicationResponse> getMine(String email) {
        User user = findUser(email);
        return applicationRepository.findAllByUser_UserIdOrderByUpdatedAtDesc(user.getUserId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApplicationResponse create(String email, ApplicationCreateRequest request) {
        User user = findUser(email);
        return applicationRepository.findByUser_UserIdAndJob_JobId(user.getUserId(), request.jobId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    JobPosting job = jobRepository.findById(request.jobId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다."));
                    return toResponse(applicationRepository.save(new UserApplication(user, job)));
                });
    }

    @Transactional
    public ApplicationResponse update(
            String email,
            Long applicationId,
            ApplicationUpdateRequest request
    ) {
        User user = findUser(email);
        UserApplication application = applicationRepository
                .findByApplicationIdAndUser_UserId(applicationId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지원 내역을 찾을 수 없습니다."));
        if (request.status() == ApplicationStatus.INTERVIEW && request.interviewDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "면접 상태에는 면접일이 필요합니다.");
        }
        application.update(request.status(), request.interviewDate());
        return toResponse(application);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private ApplicationResponse toResponse(UserApplication application) {
        JobPosting job = application.getJob();
        return new ApplicationResponse(
                application.getApplicationId(),
                job.getJobId(),
                job.getTitle(),
                job.getCompanyName(),
                application.getStatus(),
                application.getInterviewDate(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}
