package com.bankingSystem.Notification;

import com.bankingSystem.user.Role;

import java.util.ArrayList;
import java.util.List;

public class InAppNotifier implements Observer {
    private final Role role;
    private final List<NotificationEvent> inbox = new ArrayList<>();

    public InAppNotifier(Role role) {
        this.role = role;
    }

    @Override
    public boolean supports(Role role) {
        return this.role == role;
    }

    @Override
    public void update(NotificationEvent event) {
        inbox.add(event);
        System.out.println("""
        \n🔔 ===== IN-APP NOTIFICATION =====
        Time        : %s
        Target Role : %s
        Event       : %s
        Message     : %s
        Tx ID       : %s
        =================================
        """.formatted(
                event.getTimestamp(),
                event.getTargetRole(),
                event.getType(),
                event.getMessage(),
                event.getTransactionId()
        ));
    }

    public List<NotificationEvent> getInbox() {
        return new ArrayList<>(inbox);
    }
}
