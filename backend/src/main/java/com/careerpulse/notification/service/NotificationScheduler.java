package com.careerpulse.notification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "notification.mail.enabled",
        havingValue = "true"
)
public class NotificationScheduler {
    private final NotificationMailService mailService;

    public NotificationScheduler(NotificationMailService mailService) {
        this.mailService = mailService;
    }

    @Scheduled(cron = "${notification.mail.cron:0 0 9 * * *}", zone = "Asia/Seoul")
    public void sendDaily() {
        mailService.sendDaily();
    }
}
