package com.careerpulse.job.service;

import com.careerpulse.job.dto.JobBookmarkResponse;
import com.careerpulse.job.dto.JobPostingSummaryResponse;
import com.careerpulse.job.entity.JobBookmark;
import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.repository.JobBookmarkRepository;
import com.careerpulse.job.repository.JobPostingRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobBookmarkService {
    private final UserRepository userRepository;
    private final JobPostingRepository jobRepository;
    private final JobBookmarkRepository bookmarkRepository;

    public JobBookmarkService(
            UserRepository userRepository,
            JobPostingRepository jobRepository,
            JobBookmarkRepository bookmarkRepository
    ) {
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    @Transactional(readOnly = true)
    public JobBookmarkResponse status(String email, Long jobId) {
        User user = user(email);
        return bookmarkRepository.findByUser_UserIdAndJob_JobId(user.getUserId(), jobId)
                .map(this::response)
                .orElseGet(() -> new JobBookmarkResponse(jobId, false, null));
    }

    @Transactional(readOnly = true)
    public List<JobPostingSummaryResponse> list(String email) {
        User user = user(email);
        return bookmarkRepository
                .findAllByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(JobBookmark::getJob)
                .map(job -> new JobPostingSummaryResponse(
                        job.getJobId(),
                        job.getCompanyName(),
                        job.getTitle(),
                        job.getJobCategory(),
                        job.getRegion(),
                        job.getPostedDate(),
                        job.getDeadline(),
                        job.getOriginalUrl(),
                        job.getStatus()
                ))
                .toList();
    }

    @Transactional
    public JobBookmarkResponse add(String email, Long jobId) {
        User user = user(email);
        return bookmarkRepository.findByUser_UserIdAndJob_JobId(user.getUserId(), jobId)
                .map(this::response)
                .orElseGet(() -> {
                    JobPosting job = jobRepository.findById(jobId).orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "채용공고를 찾을 수 없습니다."));
                    return response(bookmarkRepository.save(new JobBookmark(user, job)));
                });
    }

    @Transactional
    public void delete(String email, Long jobId) {
        User user = user(email);
        bookmarkRepository.findByUser_UserIdAndJob_JobId(user.getUserId(), jobId)
                .ifPresent(bookmarkRepository::delete);
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private JobBookmarkResponse response(JobBookmark bookmark) {
        return new JobBookmarkResponse(
                bookmark.getJob().getJobId(),
                true,
                bookmark.getCreatedAt()
        );
    }
}
