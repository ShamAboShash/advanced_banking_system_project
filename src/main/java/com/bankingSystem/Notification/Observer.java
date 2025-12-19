package com.bankingSystem.Notification;

import com.bankingSystem.user.Role;

public interface Observer {
    boolean supports(Role role);
    void update(NotificationEvent event);
}
