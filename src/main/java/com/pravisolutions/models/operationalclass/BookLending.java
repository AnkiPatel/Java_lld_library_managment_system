package com.pravisolutions.models.operationalclass;

import com.pravisolutions.constants.AppConstants;
import com.pravisolutions.models.BookItem;
import com.pravisolutions.models.*;

import java.util.Calendar;
import java.util.Date;

/**
 * It is class which represent the operation.
 * Represents a single borrowing transaction — from the moment a book is
 * issued to a member until it is returned.
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. issuedBy (String — card number):
 *    R10 requires recording WHO issued the book. We store the card number
 *    (not a User object) to keep the audit record stable even if the
 *    issuing librarian's account is later modified or deleted.
 *
 * 2. dueDate computed at creation:
 *    dueDate = borrowDate + 15 days. Computed once and stored.
 *    On renewal, dueDate is RESET to renewalDate + 15 days.
 *
 * 3. renewalCount:
 *    Tracks how many times this lending has been renewed.
 *    Max is 2 (LibraryConstants.MAX_RENEWALS_PER_BORROW).
 *    The renew() method enforces this limit and returns false if exceeded.
 *
 * 4. isClosed:
 *    Marks this lending as completed (book returned).
 *    Closed lendings stay in borrowingHistory for audit but are removed
 *    from activeLendings.
 *
 * 5. returnDate:
 *    Null until the book is actually returned. Null = still borrowed.
 */


public class BookLending {
    private String lendingId;       // Unique ID for this transaction
    private BookItem bookItem;      // The physical copy being borrowed
    private Member member;          // The member borrowing it
    private String issuedBy;        // Card number of librarian/member who processed it
    private Date borrowDate;
    private Date dueDate;           // borrowDate + 15 days (reset on each renewal)
    private Date returnDate;        // null until returned
    private int renewalCount;       // 0, 1, or 2 — never exceeds MAX_RENEWALS_PER_BORROW
    private boolean isClosed;       // true after book is returned

    public BookLending(String lendingId, BookItem bookItem,
                       Member member, String issuedBy, Date borrowDate) {
        this.lendingId    = lendingId;
        this.bookItem     = bookItem;
        this.member       = member;
        this.issuedBy     = issuedBy;
        this.borrowDate   = borrowDate;
        this.dueDate      = computeDueDate(borrowDate);
        this.returnDate   = null;
        this.renewalCount = 0;
        this.isClosed     = false;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Date computeDueDate(Date fromDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fromDate);
        calendar.add(Calendar.DAY_OF_MONTH, AppConstants.BORROW_PERIOD_DAYS);
        return calendar.getTime();
    }

    // -----------------------------------------------------------------------
    // Core business logic
    // -----------------------------------------------------------------------

    /**
     * Attempts to renew this lending.
     *
     * On success:
     * - Increments renewalCount
     * - Resets dueDate to today + 15 days (from renewal date, not original due date)
     * - Returns true
     *
     * On failure (max renewals reached):
     * - Returns false, no state changes
     */
    public boolean renew(Date renewalDate) {
        if (renewalCount >= AppConstants.MAX_RENEWALS_PER_BORROW) {
            return false;
        }
        renewalCount++;
        dueDate = computeDueDate(renewalDate);
        return true;
    }

    /**
     * Checks if this book is currently overdue.
     * A book is overdue if today is PAST the dueDate AND it hasn't been returned.
     */
    public boolean isOverdue() {
        if (isClosed) {
            return false; // Already returned — not overdue
        }
        Date today = new Date();
        return today.after(dueDate);
    }

    /**
     * Returns the number of days this book is overdue.
     * Returns 0 if not overdue or already returned.
     * Used by IFineCalculator to compute the exact fine.
     */
    public int getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        Date today = new Date();
        long diffMillis = today.getTime() - dueDate.getTime();
        return (int) (diffMillis / (1000L * 60 * 60 * 24));
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public String getLendingId() {
        return lendingId;
    }

    public BookItem getBookItem() {
        return bookItem;
    }

    public Member getMember() {
        return member;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public Date getBorrowDate() {
        return borrowDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public boolean isClosed() {
        return isClosed;
    }

    // -----------------------------------------------------------------------
    // Setters — only returnDate and isClosed change after creation
    // -----------------------------------------------------------------------

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    public void setClosed(boolean closed) {
        this.isClosed = closed;
    }

    @Override
    public String toString() {
        return "BookLending{id='" + lendingId + "', bookItem='" + bookItem.getBookItemId()
                + "', member='" + member.getLibraryCard().getCardNumber()
                + "', renewals=" + renewalCount + ", closed=" + isClosed + "}";
    }
}
