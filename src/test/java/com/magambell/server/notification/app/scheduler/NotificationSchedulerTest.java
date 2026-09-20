package com.magambell.server.notification.app.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.magambell.server.notification.app.port.in.NotificationUseCase;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class NotificationSchedulerTest {

    @DisplayName("사장님 리뷰 묶음 알림은 매일 오전 10시 KST에 실행된다")
    @Test
    void notificationDailyOwnerReviewSummarySchedule() throws NoSuchMethodException {
        Method method = NotificationScheduler.class
                .getDeclaredMethod("notificationDailyOwnerReviewSummary");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 0 10 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    @DisplayName("사장님 리뷰 묶음 알림 스케줄러는 알림 유스케이스를 호출한다")
    @Test
    void notificationDailyOwnerReviewSummary() {
        NotificationUseCase notificationUseCase = mock(NotificationUseCase.class);
        NotificationScheduler notificationScheduler = new NotificationScheduler(notificationUseCase);

        notificationScheduler.notificationDailyOwnerReviewSummary();

        verify(notificationUseCase).notifyDailyOwnerReviewSummary();
    }
}
