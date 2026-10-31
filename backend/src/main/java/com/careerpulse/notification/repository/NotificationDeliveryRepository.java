package com.careerpulse.notification.repository;

import com.careerpulse.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
}
