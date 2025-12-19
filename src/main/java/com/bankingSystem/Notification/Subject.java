package com.bankingSystem.Notification;

public interface Subject {
    void registerObserver(Observer observer);
    void removeObserver(Observer observer);
    void notifyObservers(NotificationEvent event);
}
