package com.pravisolutions.facade;

import com.pravisolutions.models.Book;
import com.pravisolutions.models.BookItem;
import com.pravisolutions.models.Member;
import com.pravisolutions.models.operationalclass.BookLending;
import com.pravisolutions.models.operationalclass.Reservation;
import com.pravisolutions.services.BookService;
import com.pravisolutions.services.LendingService;
import com.pravisolutions.services.MemberService;
import com.pravisolutions.services.ReservationService;
import com.pravisolutions.services.searchbook.SearchStrategyFactory;

import java.util.List;

/**
 * FACADE — Single entry point into the entire Library Management System.
 *
 * FACADE PATTERN:
 * The Library class hides the complexity of 5 subsystems behind one clean API.
 * The caller (Main, a controller, a CLI) never needs to know about BookService,
 * LendingService, etc. They call Library and Library delegates.
 *
 * WHY THIS MATTERS IN AN INTERVIEW:
 * An interviewer sees one coherent API that maps directly to the requirements.
 * Each method name corresponds to a real user action from the requirements.
 * The subsystem complexity is completely invisible from the outside.
 *
 * WIRING:
 * All dependencies are injected via the constructor — there is no "new" inside
 * this class. This makes the system testable and the wiring visible in Main.
 *
 * METHOD GROUPING:
 * 1. Member self-service operations
 * 2. Librarian administrative operations
 * 3. LMS system operations (scheduled tasks)
 */

public class Library {

    private BookService bookService;
    private MemberService memberService;
    private LendingService lendingService;
    private ReservationService reservationService;


    public Library(BookService bookService,
                   MemberService memberService,
                   LendingService lendingService,
                   ReservationService reservationService) {
        this.bookService         = bookService;
        this.memberService       = memberService;
        this.lendingService      = lendingService;
        this.reservationService  = reservationService;

    }


    // =======================================================================
    // MEMBER OPERATIONS
    // =======================================================================

    /** Search catalog by title (R14 — single field search) */
    public List<Book> searchByTitle(String title) {
        //Delegation to BookService because it has access to book repository
        return this.bookService.serachBook(SearchStrategyFactory.SearchType.TITLE, title);
    }

    /** Search catalog by author (R14) */
    public List<Book> searchByAuthor(String author) {
        //Delegation to BookService because it has access to book repository
        return this.bookService.serachBook(SearchStrategyFactory.SearchType.AUTHOR, author);
    }

    /** Search catalog by subject (R14) */
    public List<Book> searchBySubject(String subject) {
        //Delegation to BookService because it has access to book repository
        return this.bookService.serachBook(SearchStrategyFactory.SearchType.SUBJECT, subject);
    }

    /** Search catalog by publication date (R14) */
    public List<Book> searchByPublicationDate(String publicationDate) {
        //Delegation to BookService because it has access to book repository
        return this.bookService.serachBook(SearchStrategyFactory.SearchType.PUBLICATION_DATE, publicationDate);
    }

    /**
     * Member borrows a BookItem directly (self-checkout).
     * issuedBy = member's own card number.
     */
    public BookLending borrowBook(String memberCardNumber, String bookItemId) {
        return lendingService.borrowBook(memberCardNumber, bookItemId, memberCardNumber);
    }

    /** Member returns a borrowed BookItem. Returns fine amount (0 if none). */
    public double returnBook(String memberCardNumber, String bookItemId) {
        return lendingService.returnBook(memberCardNumber, bookItemId);
    }

    /** Member renews a currently borrowed BookItem (max 2 renewals). */
    public BookLending renewBook(String memberCardNumber, String bookItemId) {
        return lendingService.renewBook(memberCardNumber, bookItemId);
    }

    /** Member places a reservation on a specific BookItem (R13). */
    public Reservation reserveBookItem(String memberCardNumber, String bookItemId) {
        return reservationService.reserveBookItem(memberCardNumber, bookItemId);
    }

    /** Member cancels their own reservation. */
    public void cancelReservation(String memberCardNumber, String bookItemId) {
        reservationService.cancelReservation(memberCardNumber, bookItemId);
    }

    /** Member pays outstanding fine. */
    public void payFine(String memberCardNumber, double amount) {
        Member member = memberService.getMember(memberCardNumber);
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        member.payFine(amount);
        System.out.println("[Library] Fine paid: Rs " + amount
                + " by member " + memberCardNumber
                + ". Remaining: Rs " + member.getOutstandingFine());
    }

    /** View a member's full account — details, active lendings, reservations, fines. */
    public Member viewAccount(String memberCardNumber) {
        return memberService.getMember(memberCardNumber);
    }

    // =======================================================================
    // LIBRARIAN OPERATIONS
    // =======================================================================

    /** Librarian registers a new member and issues library card. */
    public Member registerMember(String name, String email, String phone,
                                 String password, String cardNumber) {
        return memberService.registerMember(name, email, phone, password, cardNumber);
    }

    /** Librarian updates member account details. */
    public void updateMember(String cardNumber, String name,
                             String email, String phone) {
        memberService.updateMember(cardNumber, name, email, phone);
    }

    /** Librarian cancels a member's account. */
    public void cancelMembership(String cardNumber) {
        memberService.cancelMembership(cardNumber);
    }

    /** Librarian adds a new book to the catalog. */
    public Book addBook(String bookId, String isbn, String title,
                        String author, String subject, String publicationDate) {
        return bookService.addBook(bookId, isbn, title, author, subject, publicationDate);
    }

    /** Librarian updates book metadata. */
    public void editBook(String bookId, String title, String author,
                         String subject, String publicationDate) {
        bookService.editBook(bookId, title, author, subject, publicationDate);
    }

    /** Librarian removes a book from the catalog. */
    public void removeBook(String bookId) {
        bookService.removeBook(bookId);
    }

    /** Librarian adds a physical copy of a book. */
    public BookItem addBookItem(String bookId, String bookItemId, String rackLocation) {
        return bookService.addBookItem(bookId, bookItemId, rackLocation);
    }

    /** Librarian updates rack location of a physical copy. */
    public void editBookItem(String bookItemId, String rackLocation) {
        bookService.editBookItem(bookItemId, rackLocation);
    }

    /** Librarian removes a physical copy from inventory. */
    public void removeBookItem(String bookItemId) {
        bookService.removeBookItem(bookItemId);
    }

    /**
     * Librarian issues a book on behalf of a member (at the counter).
     * issuedBy = librarian's card number (recorded in audit log per R10).
     */
    public BookLending issueBookOnBehalf(String librarianCardNumber,
                                         String memberCardNumber,
                                         String bookItemId) {
        return lendingService.borrowBook(memberCardNumber, bookItemId, librarianCardNumber);
    }

    /**
     * Librarian grants a renewal for a member.
     * Uses the same renewal logic — librarian permission is expressed
     * by calling this method vs the member-facing renewBook().
     */
    public BookLending grantRenewal(String memberCardNumber, String bookItemId) {
        return lendingService.renewBook(memberCardNumber, bookItemId);
    }

    /**
     * Librarian removes a member's reservation.
     * Uses the same cancellation logic — librarian authority is expressed
     * by calling this method.
     */
    public void removeReservation(String memberCardNumber, String bookItemId) {
        reservationService.cancelReservation(memberCardNumber, bookItemId);
    }

    // =======================================================================
    // LMS SYSTEM OPERATIONS (Scheduled Tasks)
    // =======================================================================

    /**
     * LMS daily task: check all active lendings and notify overdue members (R12).
     */
    public void runOverdueCheck() {
        System.out.println("[Library/LMS] Running overdue check...");
        lendingService.checkAndNotifyOverdue();
    }

    /**
     * LMS daily task: expire reservations where member missed the 2-day pickup window.
     */
    public void runReservationExpiryCheck() {
        System.out.println("[Library/LMS] Running reservation expiry check...");
        reservationService.checkAndExpireReservations();
    }

    // =======================================================================
    // UTILITY — for display/debugging
    // =======================================================================

    public BookItem getBookItem(String bookItemId) {
        return bookService.getBookItem(bookItemId);
    }

    public Book getBook(String bookId) {
        return bookService.getBook(bookId);
    }
}
