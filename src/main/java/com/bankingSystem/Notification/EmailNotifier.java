package com.bankingSystem.Notification;
import com.bankingSystem.user.Role;

public class EmailNotifier implements Observer {

    private final Role role;

    public EmailNotifier(Role role) {
        this.role = role;
    }

    @Override
    public boolean supports(Role role) {
        return this.role == role;
    }

    @Override
    public void update(NotificationEvent event) {
        System.out.println("""
        \n📧 ===== EMAIL NOTIFICATION =====
        Time        : %s
        Target Role : %s
        Event       : %s
        Message     : %s
        Tx ID       : %s
        ================================
        """.formatted(
                event.getTimestamp(),
                event.getTargetRole(),
                event.getType(),
                event.getMessage(),
                event.getTransactionId()
        ));
    }
}
