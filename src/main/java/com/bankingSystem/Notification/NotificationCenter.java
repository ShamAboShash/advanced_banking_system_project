package com.bankingSystem.Notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationCenter implements Subject {

    private static final NotificationCenter INSTANCE = new NotificationCenter();
    public static NotificationCenter getInstance() { return INSTANCE; }

    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    // Async dispatch to avoid slowing transactions
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    private NotificationCenter() {}

    @Override
    public void registerObserver(Observer o) { observers.add(o); }

    @Override
    public void removeObserver(Observer o) { observers.remove(o); }

    @Override
    public void notifyObservers(NotificationEvent event) {
        for (Observer o : observers) {
            if (o.supports(event)) {
                pool.submit(() -> {
                    try {
                        o.update(event);
                    } catch (Exception ex) {
                        synchronized (ConsoleLock.PRINT_LOCK) {
                            System.err.println("❌ Notification observer failed: " + ex.getMessage());
                        }
                    }
                });
            }
        }
    }
}
