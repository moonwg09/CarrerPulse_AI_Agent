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
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

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
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("MAIL_FROM이 설정되지 않았습니다.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(ordered.getFirst().email());
            helper.setSubject(subject(ordered.getFirst().userName()));
            helper.setText(textBody(ordered), htmlBody(ordered));
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new MailPreparationException("채용공고 알림 메일을 만들 수 없습니다.", exception);
        }

        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findById(ordered.getFirst().userId()).orElseThrow();
            List<NotificationDelivery> deliveries = ordered.stream().map(candidate -> {
                JobPosting job = jobRepository.findById(candidate.jobId()).orElseThrow();
                return new NotificationDelivery(user, job, candidate.type());
            }).toList();
            deliveryRepository.saveAllAndFlush(deliveries);
        });
    }

    private String subject(String userName) {
        String safeName = userName == null || userName.isBlank()
                ? "회원"
                : userName.replaceAll("[\\r\\n]", " ").trim();
        return "[CareerPulse] " + safeName + " 님, 새로운 추천 공고가 있습니다.";
    }

    private String textBody(List<NotificationCandidateRepository.Candidate> candidates) {
        String userName = candidates.getFirst().userName();
        StringBuilder result = new StringBuilder()
                .append(userName == null || userName.isBlank() ? "회원" : userName)
                .append(" 님, 관심 직무와 분석 결과를 기준으로 확인한 채용공고입니다.\n\n");
        appendTextSection(result, "마감 3일 전 관심 공고", candidates, NotificationType.DEADLINE_REMINDER);
        appendTextSection(result, "신규 추천 공고", candidates, NotificationType.NEW_RECOMMENDATION);
        result.append("이 메일은 CareerPulse에서 자동으로 발송했습니다.\nCareerPulse");
        return result.toString();
    }

    private void appendTextSection(
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
                    .append(" / ").append(candidate.title())
                    .append("\n  지역: ").append(value(candidate.region()))
                    .append(" / 일치율: ").append(matchRate(candidate));
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

    private String htmlBody(List<NotificationCandidateRepository.Candidate> candidates) {
        String userName = html(value(candidates.getFirst().userName()));
        StringBuilder body = new StringBuilder("""
                <!doctype html><html lang="ko"><body style="margin:0;background:#f4f4f4;color:#111;font-family:Arial,'Noto Sans KR',sans-serif;">
                <div style="max-width:680px;margin:0 auto;background:#fff;border:1px solid #111;">
                  <div style="padding:24px;border-bottom:3px solid #111;">
                    <div style="font-size:22px;font-weight:700;letter-spacing:-0.5px;">CareerPulse</div>
                  </div>
                  <div style="padding:28px 24px 12px;">
                """);
        body.append("<p style=\"margin:0 0 10px;font-size:18px;font-weight:700;\">")
                .append(userName).append(" 님,</p>")
                .append("<p style=\"margin:0;line-height:1.7;color:#333;\">관심 직무와 분석 결과를 기준으로 확인한 채용공고입니다.</p></div>");
        appendHtmlSection(body, "마감 3일 전 관심 공고", candidates, NotificationType.DEADLINE_REMINDER);
        appendHtmlSection(body, "신규 추천 공고", candidates, NotificationType.NEW_RECOMMENDATION);
        body.append("""
                  <div style="padding:20px 24px;border-top:1px solid #bbb;background:#f5f5f5;color:#555;font-size:12px;line-height:1.6;">
                    이 메일은 CareerPulse에서 자동으로 발송했습니다.<br>CareerPulse
                  </div>
                </div></body></html>
                """);
        return body.toString();
    }

    private void appendHtmlSection(
            StringBuilder body,
            String title,
            List<NotificationCandidateRepository.Candidate> candidates,
            NotificationType type
    ) {
        List<NotificationCandidateRepository.Candidate> section = candidates.stream()
                .filter(candidate -> candidate.type() == type)
                .toList();
        if (section.isEmpty()) return;
        body.append("<div style=\"padding:18px 24px 8px;\"><h2 style=\"margin:0 0 12px;font-size:17px;\">")
                .append(html(title)).append("</h2>");
        for (var candidate : section) {
            body.append("<div style=\"border:1px solid #222;padding:16px;margin-bottom:12px;\">")
                    .append("<div style=\"font-size:13px;color:#555;margin-bottom:5px;\">")
                    .append(html(value(candidate.companyName()))).append("</div>")
                    .append("<div style=\"font-size:16px;font-weight:700;margin-bottom:12px;\">")
                    .append(html(value(candidate.title()))).append("</div>")
                    .append("<div style=\"font-size:13px;line-height:1.7;\">지역: ")
                    .append(html(value(candidate.region())))
                    .append("<br>일치율: ").append(html(matchRate(candidate)))
                    .append("<br>마감일: ").append(candidate.deadline() == null ? "미정" : candidate.deadline())
                    .append("</div>");
            if (candidate.originalUrl() != null && !candidate.originalUrl().isBlank()) {
                body.append("<p style=\"margin:14px 0 0;\"><a style=\"color:#111;font-weight:700;\" href=\"")
                        .append(html(candidate.originalUrl()))
                        .append("\">원문 공고 보기</a></p>");
            }
            body.append("</div>");
        }
        body.append("</div>");
    }

    private String matchRate(NotificationCandidateRepository.Candidate candidate) {
        if (candidate.matchRate() == null) return "분석 결과 없음";
        return candidate.matchRate().stripTrailingZeros().toPlainString() + "%";
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "미기재" : value;
    }

    private String html(String value) {
        return HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
