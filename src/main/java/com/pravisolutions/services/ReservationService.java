package com.pravisolutions.services;

import com.pravisolutions.constants.AppConstants;
import com.pravisolutions.enums.BookItemStatus;
import com.pravisolutions.enums.MemberStatus;
import com.pravisolutions.enums.TransactionType;
import com.pravisolutions.models.Book;
import com.pravisolutions.models.BookItem;
import com.pravisolutions.models.Member;
import com.pravisolutions.models.operationalclass.Reservation;
import com.pravisolutions.models.operationalclass.TransactionLog;
import com.pravisolutions.services.notification.INotificationCapability;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Handles all reservation operations: create, cancel, expiry check.
 *
 * RESPONSIBILITY:
 * ReservationService owns the lifecycle of a Reservation — from creation
 * (member places hold) to resolution (member picks up book OR reservation expires).
 *
 * KEY INVARIANT maintained by this service:
 * At most ONE active reservation per BookItem at any time (R9).
 * This is enforced in reserveBookItem() before creating a Reservation.
 *
 * R13 INTERPRETATION:
 * "If a book is unavailable" means ALL physical copies (BookItems) are
 * in BORROWED status. A member reserves a specific BookItem — not a book title.
 * When that specific item is returned, the reserving member is notified.
 *
 * EXPIRY CHECK:
 * checkAndExpireReservations() is called by LMS on a schedule (e.g., daily).
 * It finds reservations where the member was notified but hasn't picked up
 * the book within 2 days, and cancels them automatically.
 */
public class ReservationService {

    private BookService bookService;
    private MemberService memberService;
    private INotificationCapability notificationService;
    private List<TransactionLog> transactionLog;
    private int reservationCounter;
    private int logCounter;


    public ReservationService(BookService bookService,
                              MemberService memberService,
                              INotificationCapability notificationService) {
        this.bookService         = bookService;
        this.memberService       = memberService;
        this.notificationService = notificationService;
        this.transactionLog      = new ArrayList<>();
        this.reservationCounter  = 1;
        this.logCounter          = 1;
    }

    // -----------------------------------------------------------------------
    // Audit log access
    // -----------------------------------------------------------------------

    public List<TransactionLog> getTransactionLog() {
        return transactionLog;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void log(String memberCardNumber, String bookItemId,
                     TransactionType type, Date date, String remarks) {
        TransactionLog entry = new TransactionLog(
                "RLOG-" + logCounter++,
                memberCardNumber,
                bookItemId,
                type,
                date,
                remarks
        );
        transactionLog.add(entry);
    }

    // -----------------------------------------------------------------------
    // RESERVE FLOW
    // -----------------------------------------------------------------------

    /**
     * Places a reservation on a specific BookItem for a member.
     *
     * Guard conditions (in order):
     * 1. Member is ACTIVE
     * 2. BookItem exists and is BORROWED (not AVAILABLE — no need to reserve available books)
     * 3. Member does NOT already hold a copy of the same book
     * 4. Member does NOT already have an active reservation for the same book
     * 5. BookItem does NOT already have an active reservation (R9 — one at a time)
     */
    public Reservation reserveBookItem(String memberCardNumber, String bookItemId) {
        Member member = memberService.getMember(memberCardNumber);
        BookItem item = bookService.getBookItem(bookItemId);
        Book parentBook = bookService.getBook(item.getActualBookId());

        // Guard 1: Member must be active
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Member '" + memberCardNumber + "' account is not active.");
        }

        // Guard 2: BookItem must be BORROWED (reserving AVAILABLE books makes no sense)
        if (item.getStatus() == BookItemStatus.AVAILABLE) {
            throw new IllegalStateException(
                    "BookItem '" + bookItemId + "' is available. Borrow it directly instead.");
        }

        if (item.getStatus() == BookItemStatus.LOST) {
            throw new IllegalStateException(
                    "BookItem '" + bookItemId + "' is marked as LOST and cannot be reserved.");
        }

        // Guard 3: Member must not already hold a copy of this book
        if (member.alreadyHasCopyOfBook(item.getActualBookId())) {
            throw new IllegalStateException(
                    "Member '" + memberCardNumber
                            + "' already has a borrowed copy of book '" + parentBook.getTitle() + "'.");
        }

        // Guard 4: Member must not already have a reservation for this book
        if (member.alreadyReservedBook(item.getActualBookId())) {
            throw new IllegalStateException(
                    "Member '" + memberCardNumber
                            + "' already has a reservation for book '" + parentBook.getTitle() + "'.");
        }

        // Guard 5: BookItem must not already be reserved by another member (R9)
        if (item.getReservation() != null && !item.getReservation().isCancelled()) {
            throw new IllegalStateException(
                    "BookItem '" + bookItemId + "' is already reserved by another member.");
        }

        // All guards passed — create reservation
        Date today         = new Date();
        String reservationId = "RES-" + reservationCounter++;
        Reservation reservation = new Reservation(reservationId, item, member, today);

        item.setReservation(reservation);
        member.addReservation(reservation);

        // Audit log
        log(memberCardNumber, bookItemId, TransactionType.RESERVED, today,
                "Reserved book: " + parentBook.getTitle());

        System.out.println("[ReservationService] Reserved: " + reservation);
        return reservation;
    }

    // -----------------------------------------------------------------------
    // CANCEL RESERVATION
    // -----------------------------------------------------------------------

    /**
     * Cancels an active reservation for a specific member + bookItem.
     * Can be called by member (remove own reservation) or librarian.
     *
     * After cancellation:
     * - BookItem.reservation is cleared
     * - Reservation removed from member.activeReservations
     * - BookItem status: if it was RESERVED (waiting for pickup), set back to AVAILABLE
     * - Member is notified
     */
    public void cancelReservation(String memberCardNumber, String bookItemId) {
        Member member = memberService.getMember(memberCardNumber);
        BookItem item = bookService.getBookItem(bookItemId);
        Book parentBook = bookService.getBook(item.getActualBookId());

        // Find the reservation on this BookItem for this member
        Reservation reservation = item.getReservation();
        if (reservation == null || reservation.isCancelled()) {
            throw new IllegalStateException(
                    "No active reservation found on BookItem '" + bookItemId + "'.");
        }

        if (!reservation.getMember().getLibraryCard().getCardNumber()
                .equals(memberCardNumber)) {
            throw new IllegalStateException(
                    "BookItem '" + bookItemId + "' is not reserved by member '"
                            + memberCardNumber + "'.");
        }

        // Cancel it
        reservation.setCancelled(true);
        member.removeReservation(reservation);
        item.setReservation(null);

        // If the item was in RESERVED status (available for pickup), release it back
        if (item.getStatus() == BookItemStatus.RESERVED) {
            item.setStatus(BookItemStatus.AVAILABLE);
        }

        // Notify member
        notificationService.sendReservationCancelledNotification(
                member.getName(),
                member.getEmail(),
                member.getPhone(),
                parentBook.getTitle()
        );

        Date today = new Date();
        log(memberCardNumber, bookItemId, TransactionType.RESERVATION_CANCELLED, today,
                "Cancelled by: " + memberCardNumber);

        System.out.println("[ReservationService] Reservation cancelled: " + reservation);
    }
    // -----------------------------------------------------------------------
    // LMS SCHEDULED — Reservation Expiry Check
    // -----------------------------------------------------------------------

    /**
     * Scans all BookItems for expired reservations and auto-cancels them.
     * Called periodically by LMS (e.g., daily).
     *
     * A reservation expires when:
     * - The book was returned and marked available for the reserving member
     * - The member did NOT pick it up within 2 days
     *
     * On expiry:
     * - Reservation is cancelled
     * - BookItem goes back to AVAILABLE
     * - Member is notified of cancellation
     */
    public void checkAndExpireReservations() {
        List<Book> books = bookService.getAllBooks();
        for (Book book : books) {
            for (BookItem item : book.getBookItems()) {
                Reservation reservation = item.getReservation();
                if (reservation != null
                        && !reservation.isCancelled()
                        && reservation.isExpired()) {

                    Member member = reservation.getMember();

                    // Auto-cancel expired reservation
                    reservation.setCancelled(true);
                    member.removeReservation(reservation);
                    item.setReservation(null);
                    item.setStatus(BookItemStatus.AVAILABLE);

                    // Notify member
                    notificationService.sendReservationCancelledNotification(
                            member.getName(),
                            member.getEmail(),
                            member.getPhone(),
                            book.getTitle()
                    );

                    Date today = new Date();
                    log(member.getLibraryCard().getCardNumber(),
                            item.getBookItemId(),
                            TransactionType.RESERVATION_CANCELLED,
                            today,
                            "Auto-expired after " + AppConstants.RESERVATION_EXPIRY_DAYS
                                    + " days");

                    System.out.println("[ReservationService] Reservation expired for member: "
                            + member.getLibraryCard().getCardNumber());
                }
            }
        }
    }
}
