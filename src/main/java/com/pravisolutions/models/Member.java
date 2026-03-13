package com.pravisolutions.models;

import com.pravisolutions.constants.AppConstants;
import com.pravisolutions.enums.MemberStatus;
import com.pravisolutions.models.operationalclass.BookLending;
import com.pravisolutions.models.operationalclass.Reservation;

import java.util.ArrayList;
import java.util.List;

public class Member extends User{

    private MemberStatus status;
    private List<BookLending> activeLendings;      // Currently borrowed books
    private List<Reservation> activeReservations;  // Currently open reservations
    private List<BookLending> borrowingHistory;    // Full history — never shrinks
    private double outstandingFine;

    public Member(String name, String email, String phone,
                  String password, LibraryCard libraryCard) {
        super(name, email, phone, password, libraryCard);
        activeLendings = new ArrayList<>();
        activeReservations = new ArrayList<>();
        borrowingHistory = new ArrayList<>();
        status = MemberStatus.ACTIVE;
        outstandingFine = 0.0;
    }

    @Override
    public String getRole() {
        return AppConstants.MEMBER;
    }
    // -----------------------------------------------------------------------
    // Guard Conditions — business rule checks
    // -----------------------------------------------------------------------

    /**
     * A member can borrow if:
     * - Account is ACTIVE
     * - Has not reached the 10-book limit
     * - Has no outstanding fine (policy: clear fines before borrowing)
     */
    public boolean canBorrow() {
        boolean canborrow = (status == MemberStatus.ACTIVE
                && activeLendings.size() < AppConstants.MAX_BOOKS_PER_MEMBER
                && outstandingFine == 0.0);
        return canborrow;
    }

    /**
     * Checks if the member currently holds ANY physical copy of the given book.
     * Used to prevent reserving a book the member already has borrowed (R13 constraint).
     *
     * @param actualBookId The parent Book's ID (not bookItemId)
     */
    public boolean alreadyHasCopyOfBook(String actualBookId) {
        for (BookLending lending : activeLendings) {
            if (lending.getBookItem().getActualBookId().equals(actualBookId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the member already has an active reservation for any copy of
     * the given book. Prevents double-reserving the same title.
     *
     * @param bookId The parent Book's ID (not bookItemId)
     */
    public boolean alreadyReservedBook(String bookId) {
        for (Reservation reservation : activeReservations) {
            if (reservation.getBookItem().getActualBookId().equals(bookId)) {
                return true;
            }
        }
        return false;
    }
    // -----------------------------------------------------------------------
    // Lending management
    // -----------------------------------------------------------------------

    /**
     * Records a new borrow.
     * Adds to BOTH activeLendings (for limit tracking) and borrowingHistory (for audit).
     */
    public void addLending(BookLending lending) {
        activeLendings.add(lending);
        borrowingHistory.add(lending);
    }

    /**
     * Removes from active list when book is returned.
     * Does NOT remove from borrowingHistory — history is permanent.
     */
    public void removeLending(BookLending lending) {
        activeLendings.remove(lending);
    }

    // -----------------------------------------------------------------------
    // Reservation management
    // -----------------------------------------------------------------------

    public void addReservation(Reservation reservation) {
        activeReservations.add(reservation);
    }

    public void removeReservation(Reservation reservation) {
        activeReservations.remove(reservation);
    }

    // -----------------------------------------------------------------------
    // Fine management
    // -----------------------------------------------------------------------

    public void addFine(double amount) {
        this.outstandingFine += amount;
    }

    /**
     * Reduces fine by paid amount.
     * In a real system we would validate amount <= outstandingFine.
     * Kept simple here per the "no advanced concepts" constraint.
     */
    public void payFine(double amount) {
        this.outstandingFine -= amount;
        if (this.outstandingFine < 0) {
            this.outstandingFine = 0.0;
        }
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public MemberStatus getStatus() {
        return status;
    }

    public List<BookLending> getActiveLendings() {
        return activeLendings;
    }

    public List<Reservation> getActiveReservations() {
        return activeReservations;
    }

    public List<BookLending> getBorrowingHistory() {
        return borrowingHistory;
    }

    public double getOutstandingFine() {
        return outstandingFine;
    }

    // -----------------------------------------------------------------------
    // Setters
    // -----------------------------------------------------------------------

    public void setStatus(MemberStatus status) {
        this.status = status;
    }
}
