package com.pravisolutions.models;

import com.pravisolutions.enums.BookItemStatus;
import com.pravisolutions.models.operationalclass.Reservation;

/**
 * Represents a single physical copy of a Book (R4).
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. bookId back-reference:
 *    BookItem stores its parent bookId so that given only a BookItem,
 *    we can find which Book it belongs to — critical for reservation
 *    guard checks (alreadyHasCopyOfBook uses bookId, not bookItemId).
 *
 * 2. reservation field (nullable):
 *    A BookItem holds at most ONE active reservation at any time (R9).
 *    Storing it directly on BookItem makes the "is this item reserved?"
 *    check O(1) — no need to scan a separate reservation list.
 *    When null → no reservation. When set → one member has a hold.
 *
 * 3. rackLocation:
 *    Required by R2. Tells library staff where the physical book sits.
 *    Mutable — books can be moved to different racks (editBookItem).
 *
 * 4. status:
 *    The single source of truth for a copy's current state.
 *    Drives all borrow/reserve/return logic.
 */

public class BookItem {
    private String bookItemId;      // Unique physical copy ID (e.g., "ITEM-001")
    private String actualBookId;          // Parent Book's ID — back-reference
    private String rackLocation;    // Physical location (e.g., "Rack-A3")
    private BookItemStatus status;
    private Reservation reservation; // At most one active reservation; null if none

    public BookItem(String bookItemId, String abookId, String rackLocation) {
        this.bookItemId   = bookItemId;
        this.actualBookId       = abookId;
        this.rackLocation = rackLocation;
        this.status       = BookItemStatus.AVAILABLE; // New copy starts as available
        this.reservation  = null;
    }

    // -----------------------------------------------------------------------
    // Convenience method
    // -----------------------------------------------------------------------

    /**
     * True only if this copy is on the shelf and can be borrowed immediately.
     * RESERVED and BORROWED copies are NOT available for walk-in borrowing.
     */
    public boolean isAvailable() {
        return status == BookItemStatus.AVAILABLE;
    }

    // -----------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------

    public String getBookItemId() {
        return bookItemId;
    }

    public void setBookItemId(String bookItemId) {
        this.bookItemId = bookItemId;
    }

    public String getActualBookId() {
        return actualBookId;
    }

    public void setActualBookId(String bookId) {
        this.actualBookId = bookId;
    }

    public String getRackLocation() {
        return rackLocation;
    }

    public void setRackLocation(String rackLocation) {
        this.rackLocation = rackLocation;
    }

    public BookItemStatus getStatus() {
        return status;
    }

    public void setStatus(BookItemStatus status) {
        this.status = status;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }
}
