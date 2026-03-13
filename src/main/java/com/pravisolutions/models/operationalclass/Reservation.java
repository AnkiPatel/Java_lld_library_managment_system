package com.pravisolutions.models.operationalclass;


import com.pravisolutions.constants.AppConstants;
import com.pravisolutions.models.BookItem;
import com.pravisolutions.models.Member;

import java.util.Calendar;
import java.util.Date;


/**
 * It is class which represent the operation.
 * Represents a reservation placed by a member on a specific BookItem (R9, R13).
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. Reservation is on BookItem (not Book):
 *
 *    the reservation is on the physical copy.
 *
 * 2. availableDate and expiryDate are null until the book is returned:
 *    - reservedDate   : set when reservation is created
 *    - availableDate  : set when the borrowed BookItem is returned
 *    - expiryDate     : set at same time as availableDate (availableDate + 2 days)
 *    This two-phase design cleanly separates "waiting" from "ready to pick up".
 *
 * 3. isCancelled flag (not deletion):
 *    When a reservation is cancelled (by librarian or on expiry), we mark it
 *    cancelled rather than delete it. This preserves the audit trail.
 *    The Reservation is then removed from member.activeReservations and
 *    from bookItem.reservation, but the object survives in TransactionLog.
 *
 * 4. isExpired():
 *    Checks if the member failed to pick up the book within the 2-day window.
 *    Called by ReservationService.checkAndExpireReservations() (LMS scheduled task).
 */

public class Reservation {
    private String reservationId;    // Unique ID for this reservation
    private BookItem bookItem;       // The specific physical copy being reserved
    private Member member;           // The member who placed the reservation
    private Date reservedDate;       // When reservation was created
    private Date availableDate;      // When BookItem became available (null until then)
    private Date expiryDate;         // availableDate + 2 days (null until availableDate is set)
    private boolean isCancelled;

   public Reservation(String reservationId, BookItem bookItem,
                       Member member, Date reservedDate) {
        this.reservationId = reservationId;
        this.bookItem      = bookItem;
        this.member        = member;
        this.reservedDate  = reservedDate;
        this.availableDate = null;
        this.expiryDate    = null;
        this.isCancelled   = false;
    }

    // -----------------------------------------------------------------------
    // Core business logic
    // -----------------------------------------------------------------------

    /**
     * Called when the borrowed BookItem is returned and becomes available for pickup.
     * Sets availableDate to today and computes expiryDate = today + 2 days.
     *
     * After this is called, the reserving member has 2 days to pick up the book.
     */
    public void markAvailable(Date availableDate) {
        this.availableDate = availableDate;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(availableDate);
        calendar.add(Calendar.DAY_OF_MONTH, AppConstants.RESERVATION_EXPIRY_DAYS);
        this.expiryDate = calendar.getTime();
    }

    /**
     * True if the book became available but the member failed to pick it up
     * within the 2-day window.
     *
     * Returns false if:
     * - Book has not yet been returned (availableDate is null)
     * - Reservation was already cancelled
     * - Expiry date is in the future
     */
    public boolean isExpired() {
        if (isCancelled || expiryDate == null) {
            return false;
        }
        Date today = new Date();
        return today.after(expiryDate);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getReservationId() {
        return reservationId;
    }

    public BookItem getBookItem() {
        return bookItem;
    }

    public Member getMember() {
        return member;
    }

    public Date getReservedDate() {
        return reservedDate;
    }

    public Date getAvailableDate() {
        return availableDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    @Override
    public String toString() {
        return "Reservation{id='" + reservationId
                + "', bookItem='" + bookItem.getBookItemId()
                + "', member='" + member.getLibraryCard().getCardNumber()
                + "', cancelled=" + isCancelled + "}";
    }

}
