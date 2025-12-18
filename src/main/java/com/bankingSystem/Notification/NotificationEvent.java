package com.bankingSystem.Notification;

import com.bankingSystem.user.Role;
import java.time.LocalDateTime;

public class NotificationEvent {
    private final NotificationEventType type;
    private final String message;
    private final String transactionId;
    private final Role targetRole;
    private final LocalDateTime timestamp;

    public NotificationEvent(NotificationEventType type, String message, String transactionId, Role targetRole) {
        this.type = type;
        this.message = message;
        this.transactionId = transactionId;
        this.targetRole = targetRole;
        this.timestamp = LocalDateTime.now();
    }

    public NotificationEventType getType() { return type; }
    public String getMessage() { return message; }
    public String getTransactionId() { return transactionId; }
    public Role getTargetRole() { return targetRole; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
