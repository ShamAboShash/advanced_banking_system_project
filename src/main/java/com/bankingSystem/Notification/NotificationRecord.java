package com.bankingSystem.Notification;

import java.time.LocalDateTime;

public class NotificationRecord {
    public String id;
    public String userId;
    public String role; // displayName or enum name
    public String type;
    public String message;
    public String transactionId;
    public LocalDateTime createdAt;
    public boolean delivered;
    public boolean readFlag;

    public NotificationRecord() {}
}
