package com.magambell.server.notification.app.scheduler;

import com.magambell.server.notification.app.port.in.NotificationUseCase;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationScheduler {

    private static final Integer PICKUP_INTERVAL_MINUTES = 30;
    private final NotificationUseCase notificationUseCase;

    @Scheduled(cron = "0 * * * * *")
    public void notificationPickup() {
        LocalDateTime pickupTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        if (pickupTime.getMinute() % PICKUP_INTERVAL_MINUTES == 0) {
            notificationUseCase.notifyPickup(pickupTime);
        }
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void notificationDailyOwnerReviewSummary() {
        notificationUseCase.notifyDailyOwnerReviewSummary();
    }
}
