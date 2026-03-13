package com.pravisolutions.services;

import com.pravisolutions.constants.AppConstants;
import com.pravisolutions.enums.BookItemStatus;
import com.pravisolutions.enums.TransactionType;
import com.pravisolutions.models.Book;
import com.pravisolutions.models.BookItem;
import com.pravisolutions.models.Member;
import com.pravisolutions.models.operationalclass.BookLending;
import com.pravisolutions.models.operationalclass.Reservation;
import com.pravisolutions.models.operationalclass.TransactionLog;
import com.pravisolutions.services.fine.IFineCalculator;
import com.pravisolutions.services.notification.INotificationCapability;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Core service handling all borrow, return, and renewal operations.
 *
 * RESPONSIBILITY:
 * LendingService owns the full lifecycle of a BookLending — from creation
 * (borrowBook) to closure (returnBook), including renewals in between.
 * It also runs the overdue check on behalf of LMS (R12).
 *
 * KEY DESIGN — this service does NOT know about FineCalculator type or
 * NotificationService type. It holds interfaces. This is the Strategy pattern
 * and Dependency Inversion in action: high-level policy (lending) does not
 * depend on low-level detail (which fine formula, which notification channel).
 *
 * TRANSACTION LOG:
 * Every state-changing operation appends to transactionLog.
 * This satisfies R1 (complete log) and R10 (who, what, when).
 *
 * ID GENERATION:
 * Simple counter-based IDs used here for portability (no UUID library).
 * In production, UUID.randomUUID() or a DB sequence would be used.
 */

public class LendingService {

    private BookService bookService;
    private MemberService memberService;
    private IFineCalculator fineCalculator;
    private INotificationCapability notificationService;
    private List<TransactionLog> transactionLog;
    private int lendingCounter;
    private int logCounter = 1;

    public LendingService(BookService bookService,
                          MemberService memberService,
                          IFineCalculator fineCalculator,
                          INotificationCapability notificationService) {
        this.bookService          = bookService;
        this.memberService        = memberService;
        this.fineCalculator       = fineCalculator;
        this.notificationService  = notificationService;
        this.transactionLog       = new ArrayList<>();
        this.lendingCounter       = 1;
    }

    /**
     * Appends an immutable audit record to the transaction log.
     */
    private void log(String memberCardNumber, String bookItemId,
                     TransactionType type, Date date, String remarks) {
        TransactionLog entry = new TransactionLog(
                "LOG-" + logCounter++,
                memberCardNumber,
                bookItemId,
                type,
                date,
                remarks
        );
        transactionLog.add(entry);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Finds the active (not closed) lending for a specific member + bookItem.
     * Returns null if not found.
     */
    private BookLending findActiveLending(Member member, String bookItemId) {
        for (BookLending lending : member.getActiveLendings()) {
            if (lending.getBookItem().getBookItemId().equals(bookItemId)
                    && !lending.isClosed()) {
                return lending;
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // BORROW FLOW
    // -----------------------------------------------------------------------
    /**
     * Issues a BookItem to a member.
     *
     * Guard conditions checked (in order):
     * 1. Member exists and is ACTIVE
     * 2. Member can borrow (limit + fine check)
     * 3. BookItem exists
     * 4. BookItem is AVAILABLE, OR it is RESERVED by THIS member
     * 5. If RESERVED by another member → reject
     *
     * issuedByCardNumber: card number of whoever is processing this
     * (librarian issuing on behalf, or member themselves at self-checkout)
     */
    public BookLending borrowBook(String memberCardNumber,
                                  String bookItemId,
                                  String issuedByCardNumber) {
        Member member   = memberService.getMember(memberCardNumber);
        BookItem item   = bookService.getBookItem(bookItemId);

        // Guard 1: Member must be active
        if (!member.canBorrow()) {
            throw new IllegalStateException(
                    "Member '" + memberCardNumber + "' cannot borrow. "
                            + "Check: active status, borrow limit (max 10), outstanding fines.");
        }

        // Guard 2: BookItem must be AVAILABLE or RESERVED by this member
        if (item.getStatus() == BookItemStatus.BORROWED) {
            throw new IllegalStateException(
                    "BookItem '" + bookItemId + "' is currently borrowed by another member.");
        }

        if (item.getStatus() == BookItemStatus.RESERVED) {
            Reservation reservation = item.getReservation();
            if (reservation == null
                    || !reservation.getMember().getLibraryCard().getCardNumber()
                    .equals(memberCardNumber)) {
                throw new IllegalStateException(
                        "BookItem '" + bookItemId + "' is reserved by another member.");
            }
            // This member's reservation is being fulfilled — clear it
            reservation.setCancelled(true);
            member.removeReservation(reservation);
            item.setReservation(null);
        }

        if (item.getStatus() == BookItemStatus.LOST) {
            throw new IllegalStateException(
                    "BookItem '" + bookItemId + "' is marked as LOST.");
        }

        // All guards passed — create the lending
        Date today       = new Date();
        String lendingId = "LEND-" + lendingCounter++;
        BookLending lending = new BookLending(lendingId, item, member,
                issuedByCardNumber, today);

        item.setStatus(BookItemStatus.BORROWED);
        member.addLending(lending);

        // Audit log
        log(memberCardNumber, bookItemId, TransactionType.BORROWED, today,
                "Issued by: " + issuedByCardNumber);

        System.out.println("[LendingService] Borrowed: " + lending);
        return lending;
    }
    // -----------------------------------------------------------------------
    // RETURN FLOW
    // -----------------------------------------------------------------------

    /**
     * Processes the return of a borrowed BookItem.
     *
     * Flow:
     * 1. Find the active lending for this member + item
     * 2. Calculate fine if overdue
     * 3. Close the lending
     * 4. Check if BookItem has a pending reservation
     *    - YES → mark RESERVED, notify reserving member
     *    - NO  → mark AVAILABLE
     * 5. Log transaction
     *
     * Returns the fine amount (0.0 if not overdue).
     */
    public double returnBook(String memberCardNumber, String bookItemId) {
        Member member   = memberService.getMember(memberCardNumber);
        BookItem item   = bookService.getBookItem(bookItemId);

        // Find the active lending for this member + this item
        BookLending lending = findActiveLending(member, bookItemId);
        if (lending == null) {
            throw new IllegalStateException(
                    "No active lending found for member '" + memberCardNumber
                            + "' and bookItem '" + bookItemId + "'.");
        }

        Date today  = new Date();
        double fineAmount = 0.0;

        // Calculate and apply fine if overdue
        if (lending.isOverdue()) {
            fineAmount = fineCalculator.calculate(lending.getDaysOverdue());
            member.addFine(fineAmount);
            System.out.println("[LendingService] Fine applied: Rs " + fineAmount
                    + " to member " + memberCardNumber);
        }

        // Close the lending
        lending.setReturnDate(today);
        lending.setClosed(true);
        member.removeLending(lending);

        // Determine BookItem's next status
        Reservation pendingReservation = item.getReservation();
        if (pendingReservation != null && !pendingReservation.isCancelled()) {
            // A member is waiting — mark as RESERVED and notify them
            item.setStatus(BookItemStatus.RESERVED);
            pendingReservation.markAvailable(today);

            Member reservingMember = pendingReservation.getMember();
            Book parentBook = bookService.getBook(item.getActualBookId());
            notificationService.sendReservationAvailableNotification(
                    reservingMember.getName(),
                    reservingMember.getEmail(),
                    reservingMember.getPhone(),
                    parentBook.getTitle()
            );
            System.out.println("[LendingService] Reservation notified for: "
                    + reservingMember.getLibraryCard().getCardNumber());
        } else {
            // No reservation — book goes back on shelf
            item.setStatus(BookItemStatus.AVAILABLE);
        }

        // Notify returning member if they have a fine
        if (fineAmount > 0) {
            Book parentBook = bookService.getBook(item.getActualBookId());
            notificationService.sendOverdueNotification(
                    member.getName(),
                    member.getEmail(),
                    member.getPhone(),
                    parentBook.getTitle()
            );
        }

        // Audit log
        String remarks = fineAmount > 0 ? "Fine: Rs " + fineAmount : "No fine";
        log(memberCardNumber, bookItemId, TransactionType.RETURNED, today, remarks);

        System.out.println("[LendingService] Returned: bookItem=" + bookItemId
                + ", fine=" + fineAmount);
        return fineAmount;
    }

    // -----------------------------------------------------------------------
    // RENEW FLOW
    // -----------------------------------------------------------------------

    /**
     * Renews a currently borrowed BookItem for a member.
     *
     * Guard conditions:
     * 1. Active lending exists for this member + item
     * 2. Renewal count < MAX_RENEWALS_PER_BORROW (2)
     *
     * On success: dueDate resets to today + 15 days.
     */
    public BookLending renewBook(String memberCardNumber, String bookItemId) {
        Member member = memberService.getMember(memberCardNumber);
        bookService.getBookItem(bookItemId); // validate item exists

        BookLending lending = findActiveLending(member, bookItemId);
        if (lending == null) {
            throw new IllegalStateException(
                    "No active lending found for member '" + memberCardNumber
                            + "' and bookItem '" + bookItemId + "'.");
        }

        Date today = new Date();
        boolean renewed = lending.renew(today);
        if (!renewed) {
            throw new IllegalStateException(
                    "Maximum renewals (" + AppConstants.MAX_RENEWALS_PER_BORROW
                            + ") reached for lending: " + lending.getLendingId());
        }

        // Audit log
        log(memberCardNumber, bookItemId, TransactionType.RENEWED, today,
                "Renewal #" + lending.getRenewalCount()
                        + ". New due date: " + lending.getDueDate());

        System.out.println("[LendingService] Renewed: " + lending);
        return lending;
    }
    // -----------------------------------------------------------------------
    // LMS SCHEDULED — Overdue Check
    // -----------------------------------------------------------------------

    /**
     * Scans all members' active lendings and sends overdue notifications.
     * Called periodically by LMS (e.g., daily scheduled job).
     * Satisfies R12 (notify members if book not returned by due date).
     */
    public void checkAndNotifyOverdue() {
        List<Member> members = memberService.getAllMembers();
        for (Member member : members) {
            for (BookLending lending : member.getActiveLendings()) {
                if (lending.isOverdue()) {
                    Book parentBook = bookService.getBook(lending.getBookItem().getActualBookId());
                    notificationService.sendOverdueNotification(
                            member.getName(),
                            member.getEmail(),
                            member.getPhone(),
                            parentBook.getTitle()
                    );
                    System.out.println("[LendingService] Overdue notification sent to: "
                            + member.getLibraryCard().getCardNumber());
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Audit log access
    // -----------------------------------------------------------------------

    public List<TransactionLog> getTransactionLog() {
        return transactionLog;
    }
}
