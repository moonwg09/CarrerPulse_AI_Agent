package com.careerpulse.notification.service;

import com.careerpulse.job.entity.JobPosting;
import com.careerpulse.job.repository.JobPostingRepository;
import com.careerpulse.notification.entity.NotificationDelivery;
import com.careerpulse.notification.entity.NotificationType;
import com.careerpulse.notification.repository.NotificationCandidateRepository;
import com.careerpulse.notification.repository.NotificationDeliveryRepository;
import com.careerpulse.user.entity.User;
import com.careerpulse.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationMailService {
    private final NotificationCandidateRepository candidateRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final JobPostingRepository jobRepository;
    private final JavaMailSender mailSender;
    private final String from;
    private final TransactionTemplate transactionTemplate;

    public NotificationMailService(
            NotificationCandidateRepository candidateRepository,
            NotificationDeliveryRepository deliveryRepository,
            UserRepository userRepository,
            JobPostingRepository jobRepository,
            JavaMailSender mailSender,
            TransactionTemplate transactionTemplate,
            @Value("${notification.mail.from:}") String from
    ) {
        this.candidateRepository = candidateRepository;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.mailSender = mailSender;
        this.transactionTemplate = transactionTemplate;
        this.from = from;
    }

    public int sendDaily() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Map<Long, List<NotificationCandidateRepository.Candidate>> byUser =
                candidateRepository.find(today).stream()
                        .collect(Collectors.groupingBy(
                                NotificationCandidateRepository.Candidate::userId
                        ));
        int sent = 0;
        for (List<NotificationCandidateRepository.Candidate> candidates : byUser.values()) {
            if (!candidates.isEmpty()) {
                sendOne(candidates);
                sent++;
            }
        }
        return sent;
    }

    void sendOne(List<NotificationCandidateRepository.Candidate> candidates) {
        List<NotificationCandidateRepository.Candidate> ordered = candidates.stream()
                .sorted(Comparator.comparing(candidate ->
                        candidate.type() == NotificationType.DEADLINE_REMINDER ? 0 : 1))
                .toList();
        SimpleMailMessage message = new SimpleMailMessage();
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("MAIL_FROM이 설정되지 않았습니다.");
        }
        message.setFrom(from);
        message.setTo(ordered.getFirst().email());
        message.setSubject("[CareerPulse] 오늘의 채용공고 안내");
        message.setText(body(ordered));
        mailSender.send(message);

        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findById(ordered.getFirst().userId()).orElseThrow();
            List<NotificationDelivery> deliveries = ordered.stream().map(candidate -> {
                JobPosting job = jobRepository.findById(candidate.jobId()).orElseThrow();
                return new NotificationDelivery(user, job, candidate.type());
            }).toList();
            deliveryRepository.saveAllAndFlush(deliveries);
        });
    }

    private String body(List<NotificationCandidateRepository.Candidate> candidates) {
        StringBuilder result = new StringBuilder("CareerPulse 채용공고 안내입니다.\n\n");
        appendSection(result, "마감 3일 전 관심 공고", candidates, NotificationType.DEADLINE_REMINDER);
        appendSection(result, "신규 추천 공고", candidates, NotificationType.NEW_RECOMMENDATION);
        return result.toString();
    }

    private void appendSection(
            StringBuilder body,
            String title,
            List<NotificationCandidateRepository.Candidate> candidates,
            NotificationType type
    ) {
        List<NotificationCandidateRepository.Candidate> section = candidates.stream()
                .filter(candidate -> candidate.type() == type)
                .toList();
        if (section.isEmpty()) return;
        body.append("[").append(title).append("]\n");
        for (var candidate : section) {
            body.append("- ").append(candidate.companyName())
                    .append(" / ").append(candidate.title());
            if (candidate.deadline() != null) {
                body.append(" / 마감 ").append(candidate.deadline());
            }
            if (candidate.originalUrl() != null && !candidate.originalUrl().isBlank()) {
                body.append("\n  ").append(candidate.originalUrl());
            }
            body.append("\n");
        }
        body.append("\n");
    }
}
