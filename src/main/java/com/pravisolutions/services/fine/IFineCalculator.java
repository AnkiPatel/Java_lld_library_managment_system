package com.pravisolutions.services.fine;

/**
 * Strategy interface for fine calculation.
 *
 * WHY INTERFACE (Strategy Pattern):
 * The library can choose between PerDay or PerWeek fine strategies.
 * By coding to this interface, LendingService never needs to change
 * when the fine strategy changes — we simply swap the implementation.
 * This satisfies the Open/Closed Principle.
 *
 * Implementations:
 *   - PerDayFineCalculator  : Rs 2.0 per overdue day
 *   - PerWeekFineCalculator : Rs 10.0 per overdue week (or part thereof)
 */
public interface IFineCalculator {
    /**
     * Calculates the total fine for a given number of overdue days.
     *
     * @param overdueDays Number of days past the due date. Must be >= 0.
     *                    If 0, fine should be 0.
     * @return Total fine amount in rupees.
     */
    double calculate(int overdueDays);
}
