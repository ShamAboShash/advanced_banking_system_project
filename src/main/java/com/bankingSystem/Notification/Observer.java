package com.bankingSystem.Notification;

import com.bankingSystem.user.Role;

public interface Observer {
    boolean supports(NotificationEvent event);
    void update(NotificationEvent event);
}
