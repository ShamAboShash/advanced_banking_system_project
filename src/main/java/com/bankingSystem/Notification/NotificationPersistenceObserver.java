package com.bankingSystem.Notification;

import com.bankingSystem.Database.NotificationDAO;

public class NotificationPersistenceObserver implements Observer {
    private final NotificationDAO dao = new NotificationDAO();

    @Override
    public boolean supports(NotificationEvent event) {
        return event != null && event.getTargetUserId() != null && event.getTargetRole() != null;
    }

    @Override
    public void update(NotificationEvent event) {
        NotificationRecord rec = new NotificationRecord();
        rec.userId = event.getTargetUserId();
        rec.role = event.getTargetRole().getDisplayName();
        rec.type = event.getType().name();
        rec.message = event.getMessage();
        rec.transactionId = event.getTransactionId();
        rec.createdAt = event.getTimestamp();
        rec.delivered = false;
        rec.readFlag = false;
        dao.save(rec);
    }
}
