package com.pravisolutions.enums;

/**
 * All possible actions that are recorded in the transaction audit log.
 * Every borrow, return, renewal, reservation, and fine event is captured
 * using one of these types — satisfying R1 and R10.
 */
public enum TransactionType {
    BORROWED,
    RETURNED,
    RENEWED,
    RESERVED,
    RESERVATION_CANCELLED,
    FINE_PAID
}
