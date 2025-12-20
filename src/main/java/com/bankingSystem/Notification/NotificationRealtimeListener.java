package com.bankingSystem.Notification;

import com.bankingSystem.Database.NotificationDAO;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationRealtimeListener implements Runnable {

    private final String userId;
    private final NotificationDAO dao = new NotificationDAO();
    private volatile boolean running = true;

    public NotificationRealtimeListener(String userId) {
        this.userId = userId;
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        while (running) {
            try {
                // polling style (console-friendly "real-time")
                List<NotificationRecord> list = dao.loadUndelivered(userId);

                if (!list.isEmpty()) {
                    synchronized (ConsoleLock.PRINT_LOCK) {
                        for (var n : list) {
                            System.out.println("""
                                \n🔔 ===== REAL-TIME IN-APP =====
                                Time  : %s
                                Type  : %s
                                Msg   : %s
                                TxId  : %s
                                =============================
                                """.formatted(n.createdAt, n.type, n.message, n.transactionId));
                        }
                    }
                    dao.markDelivered(list.stream().map(x -> x.id).collect(Collectors.toList()));
                }

                Thread.sleep(1200); // أقل ضغط من 700ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                synchronized (ConsoleLock.PRINT_LOCK) {
                    System.err.println("Listener error: " + ex.getMessage());
                }
                try { Thread.sleep(1500); } catch (InterruptedException ignored) { return; }
            }
        }
    }
}
