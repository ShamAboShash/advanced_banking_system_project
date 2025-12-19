package com.bankingSystem.Notification;

import com.bankingSystem.Database.NotificationDAO;
import com.bankingSystem.user.Role;
public class InAppNotifier implements Observer {

    private final String sessionUserId;
    private final Role sessionRole;

    public InAppNotifier(String sessionUserId, Role sessionRole) {
        this.sessionUserId = sessionUserId;
        this.sessionRole = sessionRole;
    }

    @Override
    public boolean supports(NotificationEvent event) {
        return event != null
                && sessionRole == event.getTargetRole()
                && sessionUserId != null
                && sessionUserId.equals(event.getTargetUserId());
    }

    @Override
    public void update(NotificationEvent event) {
        synchronized (ConsoleLock.PRINT_LOCK) {
            System.out.println("""
                \n🔔 IN-APP (SESSION) -> userId=%s | %s | %s
                """.formatted(event.getTargetUserId(), event.getType(), event.getMessage()));
        }
    }
}
