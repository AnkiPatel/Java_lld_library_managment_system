package com.pravisolutions.models;

import java.util.Date;
/**
 * Represents the physical library card assigned to every user (R5, R6).
 *
 * Both Members and Librarians hold a LibraryCard.
 * The cardNumber is the primary identifier used across all transactions —
 * it is how the system knows "who" performed an action (R10).
 *
 * isActive flag allows card deactivation without deleting the record,
 * which preserves audit history.
 */
public class LibraryCard {

    private String cardNumber;
    private Date issuedDate;
    private boolean isActive;

    public LibraryCard(String cardNumber, Date issuedDate) {
        this.cardNumber = cardNumber;
        this.issuedDate = issuedDate;
        this.isActive   = true;  // Card is active by default on creation
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getCardNumber() {
        return cardNumber;
    }

    public Date getIssuedDate() {
        return issuedDate;
    }

    public boolean isActive() {
        return isActive;
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setActive(boolean active) {
        this.isActive = active;
    }

    @Override
    public String toString() {
        return "LibraryCard{cardNumber='" + cardNumber + "', isActive=" + isActive + "}";
    }

}
