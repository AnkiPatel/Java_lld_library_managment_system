package com.pravisolutions.enums;


/**
 * Represents the account status of a library member.
 *
 * ACTIVE       — Member is in good standing; can borrow, reserve, etc.
 * CANCELLED    — Membership has been voluntarily or forcibly terminated.
 * BLACKLISTED  — Member is blocked due to policy violations (e.g., unpaid fines).
 */
public enum MemberStatus {
    ACTIVE,
    CANCELLED,
    BLACKLISTED
}
