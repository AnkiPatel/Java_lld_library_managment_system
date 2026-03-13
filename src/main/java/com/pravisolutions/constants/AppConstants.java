package com.pravisolutions.constants;

/**
 * Central place for all library policy constants.
 *
 * */
public final class AppConstants {

    public static final String LIBRARIAN = "LIBRARIAN";
    public static final String MEMBER = "MEMBER";

    /** Maximum number of books a member can have borrowed at any one time. */
    public static final int MAX_BOOKS_PER_MEMBER = 10;

    /** Number of days a member can keep a borrowed book before it is overdue. */
    public static final int BORROW_PERIOD_DAYS = 15;

    /** Maximum number of times a single borrowing can be renewed. */
    public static final int MAX_RENEWALS_PER_BORROW = 2;

    // -----------------------------------------------------------------------
    // Reservation Rules
    // -----------------------------------------------------------------------

    /**
     * Once a reserved book is returned and becomes available, the reserving
     * member has this many days to pick it up before the reservation expires.
     */
    public static final int RESERVATION_EXPIRY_DAYS = 2;

    // -----------------------------------------------------------------------
    // Fine Amounts
    // -----------------------------------------------------------------------

    /** Fine charged per overdue day (used by PerDayFineCalculator). */
    public static final double FINE_PER_DAY = 2.0;

    /**
     * Fine charged per overdue week or part thereof
     * (used by PerWeekFineCalculator).
     */
    public static final double FINE_PER_WEEK = 10.0;

    // -----------------------------------------------------------------------
    // Private constructor — no instantiation allowed
    // -----------------------------------------------------------------------
    private AppConstants() {
        // utility class
    }
}
