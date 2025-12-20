package com.bankingSystem.Notification;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NotificationCenterTest {

    @Test
    void notifyObservers_shouldCallOnlySupportingObservers() throws Exception {
        NotificationCenter center = NotificationCenter.getInstance();

        // مهم: نظّفي observers من اختبارات سابقة (إذا ركضتي عدة مرات)
        // ما عندك method clear، فخلينا نعمل remove بعد التسجيل
        AtomicInteger called = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Observer good = new Observer() {
            @Override public boolean supports(NotificationEvent e) { return true; }
            @Override public void update(NotificationEvent e) {
                called.incrementAndGet();
                latch.countDown();
            }
        };

        Observer notSupported = new Observer() {
            @Override public boolean supports(NotificationEvent e) { return false; }
            @Override public void update(NotificationEvent e) { called.incrementAndGet(); }
        };

        center.registerObserver(good);
        center.registerObserver(notSupported);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_COMPLETED,
                "ok",
                "tx1",
                com.bankingSystem.user.Role.CUSTOMER,
                "u1"
        );

        center.notifyObservers(ev);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Observer did not receive event in time");
        assertEquals(1, called.get(), "Only supporting observer should be called");

        // تنظيف
        center.removeObserver(good);
        center.removeObserver(notSupported);
    }

    @Test
    void notifyObservers_whenOneObserverThrows_shouldStillNotifyOthers() throws Exception {
        NotificationCenter center = NotificationCenter.getInstance();

        CountDownLatch latch = new CountDownLatch(1);

        Observer bad = new Observer() {
            @Override public boolean supports(NotificationEvent e) { return true; }
            @Override public void update(NotificationEvent e) { throw new RuntimeException("boom"); }
        };

        Observer good = new Observer() {
            @Override public boolean supports(NotificationEvent e) { return true; }
            @Override public void update(NotificationEvent e) { latch.countDown(); }
        };

        center.registerObserver(bad);
        center.registerObserver(good);

        NotificationEvent ev = new NotificationEvent(
                NotificationEventType.TRANSACTION_FAILED,
                "fail",
                "tx2",
                com.bankingSystem.user.Role.CUSTOMER,
                "u1"
        );

        center.notifyObservers(ev);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Good observer should still be notified");

        center.removeObserver(bad);
        center.removeObserver(good);
    }
}
