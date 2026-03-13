package com.pravisolutions;

import com.pravisolutions.facade.Library;
import com.pravisolutions.models.Book;
import com.pravisolutions.models.BookItem;
import com.pravisolutions.models.Member;
import com.pravisolutions.models.operationalclass.BookLending;
import com.pravisolutions.models.operationalclass.Reservation;
import com.pravisolutions.repository.IBookRepository;
import com.pravisolutions.repository.IMemberRepository;
import com.pravisolutions.repository.InMemoryBookRepository;
import com.pravisolutions.repository.InMemoryMemberRepository;
import com.pravisolutions.services.BookService;
import com.pravisolutions.services.LendingService;
import com.pravisolutions.services.MemberService;
import com.pravisolutions.services.ReservationService;
import com.pravisolutions.services.fine.IFineCalculator;
import com.pravisolutions.services.fine.PerDayFineCalculator;
import com.pravisolutions.services.notification.CompositeNotificationService;
import com.pravisolutions.services.notification.EmailNotificationService;
import com.pravisolutions.services.notification.SmsNotificationService;

import java.util.List;

/**
 * Entry point and end-to-end demonstration of the Library Management System.
 *
 * WIRING PHILOSOPHY (Dependency Injection by hand):
 * We build every object from the bottom up, injecting dependencies explicitly.
 * This is what a DI framework (Spring) does automatically — here we do it
 * manually to stay framework-free and make every dependency visible.
 *
 * Build order:
 *   Repositories → Services → Facade
 *
 * DEMO SCENARIO (from blueprint Section 12):
 *   1.  Setup fine strategy and notification channels
 *   2.  Wire all services and create Library facade
 *   3.  Librarian registers members: Anki and Bob
 *   4.  Librarian adds Book "Clean Code" with 2 physical copies
 *   5.  Anki searches by author "Robert Martin"
 *   6.  Anki borrows BookItem-1
 *   7.  Anki renews BookItem-1 (renewal #1)
 *   8.  Bob reserves BookItem-1 (it is borrowed by Anki)
 *   9.  Anki returns BookItem-1 → triggers reservation-available notification to Bob
 *   10. Bob borrows BookItem-1 (fulfils his reservation)
 *   11. LMS runs overdue check → no overdues
 *   12. LMS runs reservation expiry check
 *   13. Bob pays fine (if any)
 *   14. View accounts
 */

public class AppLMS {
    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  Library Management System — LLD Demo");
        System.out.println("=======================================================\n");

        // -------------------------------------------------------------------
        // STEP 1: Setup fine strategy
        // Fine strategy is fixed per library (PerDay here — Rs 2.0/day)
        // To switch to PerWeek, replace with: new PerWeekFineCalculator()
        // Zero changes elsewhere in the system.
        // -------------------------------------------------------------------
        IFineCalculator fineCalculator = new PerDayFineCalculator();
        System.out.println("[Setup] Fine strategy: PerDay (Rs 2.0/day)\n");

        // -------------------------------------------------------------------
        // STEP 2: Setup notification — Email + SMS via Composite
        // -------------------------------------------------------------------
        CompositeNotificationService notificationService = new CompositeNotificationService();
        notificationService.addChannel(new EmailNotificationService());
        notificationService.addChannel(new SmsNotificationService());
        System.out.println("[Setup] Notification: Email + SMS (Composite)\n");

        // -------------------------------------------------------------------
        // STEP 3: Build repositories
        // -------------------------------------------------------------------
        IBookRepository bookRepository     = new InMemoryBookRepository();
        IMemberRepository memberRepository = new InMemoryMemberRepository();

        // -------------------------------------------------------------------
        // STEP 4: Build services — inject repositories
        // -------------------------------------------------------------------
        BookService bookService       = new BookService(bookRepository);
        MemberService memberService   = new MemberService(memberRepository);

        LendingService lendingService = new LendingService(
                bookService, memberService, fineCalculator, notificationService);

        ReservationService reservationService = new ReservationService(
                bookService, memberService, notificationService);

        // -------------------------------------------------------------------
        // STEP 5: Build Library facade — wire all services together
        // -------------------------------------------------------------------
        Library library = new Library(
                bookService, memberService,
                lendingService, reservationService);

        System.out.println("=======================================================");
        System.out.println("  SCENARIO START");
        System.out.println("=======================================================\n");

        // -------------------------------------------------------------------
        // ACTION 1: Librarian registers members
        // -------------------------------------------------------------------
        System.out.println("--- [1] Registering members ---");
        Member anki = library.registerMember(
                "Anki", "anki@email.com", "9000000001", "pass123", "CARD-001");
        Member bob = library.registerMember(
                "Bob", "bob@email.com", "9000000002", "pass456", "CARD-002");
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 2: Librarian adds book and physical copies
        // -------------------------------------------------------------------
        System.out.println("--- [2] Adding book and copies to catalog ---");
        Book cleanCode = library.addBook(
                "BOOK-001", "978-0132350884",
                "Clean Code", "Robert C. Martin",
                "Software Engineering", "2008");

        BookItem item1 = library.addBookItem("BOOK-001", "ITEM-001", "Rack-A1");
        BookItem item2 = library.addBookItem("BOOK-001", "ITEM-002", "Rack-A2");
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 3: Anki searches by author
        // -------------------------------------------------------------------
        System.out.println("--- [3] Anki searches by author 'Robert Martin' ---");
        List<Book> searchResults = library.searchByAuthor("Robert");
        for (Book book : searchResults) {
            System.out.println("  Found: " + book);
        }
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 4: Anki borrows ITEM-001
        // -------------------------------------------------------------------
        System.out.println("--- [4] Anki borrows ITEM-001 ---");
        BookLending ankiLending = library.borrowBook("CARD-001", "ITEM-001");
        System.out.println("  Status of ITEM-001: " + library.getBookItem("ITEM-001").getStatus());
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 5: Anki renews ITEM-001 (renewal #1)
        // -------------------------------------------------------------------
        System.out.println("--- [5] Anki renews ITEM-001 (renewal #1) ---");
        BookLending renewed = library.renewBook("CARD-001", "ITEM-001");
        System.out.println("  Renewal count: " + renewed.getRenewalCount());
        System.out.println("  New due date:  " + renewed.getDueDate());
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 6: Bob reserves ITEM-001 (currently borrowed by Anki)
        // -------------------------------------------------------------------
        System.out.println("--- [6] Bob reserves ITEM-001 (borrowed by Anki) ---");
        Reservation bobReservation = library.reserveBookItem("CARD-002", "ITEM-001");
        System.out.println("  Reservation: " + bobReservation);
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 7: Anki returns ITEM-001
        // → Fine = 0 (just borrowed, not overdue in this demo)
        // → Bob gets notified: reservation available
        // -------------------------------------------------------------------
        System.out.println("--- [7] Anki returns ITEM-001 ---");
        double fine = library.returnBook("CARD-001", "ITEM-001");
        System.out.println("  Fine charged: Rs " + fine);
        System.out.println("  Status of ITEM-001: " + library.getBookItem("ITEM-001").getStatus());
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 8: Bob borrows ITEM-001 (fulfils his reservation)
        // -------------------------------------------------------------------
        System.out.println("--- [8] Bob borrows ITEM-001 (fulfils reservation) ---");
        BookLending bobLending = library.borrowBook("CARD-002", "ITEM-001");
        System.out.println("  Status of ITEM-001: " + library.getBookItem("ITEM-001").getStatus());
        System.out.println("  Bob's reservation cleared: "
                + library.viewAccount("CARD-002").getActiveReservations().isEmpty());
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 9: LMS runs overdue check — no overdues expected
        // -------------------------------------------------------------------
        System.out.println("--- [9] LMS: Overdue check ---");
        library.runOverdueCheck();
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 10: LMS runs reservation expiry check
        // -------------------------------------------------------------------
        System.out.println("--- [10] LMS: Reservation expiry check ---");
        library.runReservationExpiryCheck();
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 11: Guard condition demo — Anki tries to borrow ITEM-001
        //            (already borrowed by Bob)
        // -------------------------------------------------------------------
        System.out.println("--- [11] Guard demo: Anki tries to borrow ITEM-001 (borrowed by Bob) ---");
        try {
            library.borrowBook("CARD-001", "ITEM-001");
        } catch (IllegalStateException e) {
            System.out.println("  Correctly rejected: " + e.getMessage());
        }
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 12: Guard condition demo — Bob tries to renew 3 times
        // -------------------------------------------------------------------
        System.out.println("--- [12] Guard demo: Bob tries to renew past max limit ---");
        library.renewBook("CARD-002", "ITEM-001");  // Renewal #1
        library.renewBook("CARD-002", "ITEM-001");  // Renewal #2
        try {
            library.renewBook("CARD-002", "ITEM-001");  // Should fail
        } catch (IllegalStateException e) {
            System.out.println("  Correctly rejected: " + e.getMessage());
        }
        System.out.println();

        // -------------------------------------------------------------------
        // ACTION 13: View final account states
        // -------------------------------------------------------------------
        System.out.println("--- [13] Final account states ---");
        Member ankiFinal = library.viewAccount("CARD-001");
        System.out.println("  Anki — active lendings: " + ankiFinal.getActiveLendings().size()
                + ", fine: Rs " + ankiFinal.getOutstandingFine()
                + ", history: " + ankiFinal.getBorrowingHistory().size() + " records");

        Member bobFinal = library.viewAccount("CARD-002");
        System.out.println("  Bob  — active lendings: " + bobFinal.getActiveLendings().size()
                + ", fine: Rs " + bobFinal.getOutstandingFine()
                + ", renewals on ITEM-001: " + bobLending.getRenewalCount());
        System.out.println();

        System.out.println("=======================================================");
        System.out.println("  DEMO COMPLETE");
        System.out.println("=======================================================");
    }
}
