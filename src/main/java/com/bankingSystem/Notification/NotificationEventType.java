package com.bankingSystem.Notification;

public enum NotificationEventType {

    // ===== Transactions (Workflow / Approval) =====
    TRANSACTION_SUBMITTED,
    TRANSACTION_VALIDATION_FAILED,
    TRANSACTION_PENDING_MANAGER_APPROVAL,
    TRANSACTION_APPROVED_BY_MANAGER,
    TRANSACTION_REJECTED_BY_MANAGER,

    // ===== Transactions (Result) =====
    TRANSACTION_COMPLETED,
    TRANSACTION_FAILED,

    // ===== Alerts =====
    LARGE_TRANSACTION_ALERT,
    SUSPICIOUS_ACTIVITY_ALERT,

    // ===== Account lifecycle =====
    ACCOUNT_CREATED,
    ACCOUNT_CLOSED,

    // ===== Account state transitions (State Pattern) =====
    ACCOUNT_FROZEN,
    ACCOUNT_SUSPENDED,
    ACCOUNT_ACTIVATED,

    // ===== Balance / thresholds =====
    LOW_BALANCE_WARNING,
    OVERDRAFT_USED,

    // ===== Security / Access control (Proxy) =====
    ACCESS_DENIED,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    PASSWORD_CHANGED,

    // ===== System / Admin =====
    DAILY_REPORT_READY,
    AUDIT_LOG_EVENT
}
