package com.pravisolutions.services.fine;

import com.pravisolutions.constants.AppConstants;
/**
 * Fine strategy: charge Rs 2.0 for every overdue day.
 *
 * Example:
 *   overdueDays = 5 → fine = 5 * 2.0 = Rs 10.0
 *   overdueDays = 0 → fine = Rs 0.0
 *
 * This is the simpler of the two strategies — directly proportional to days late.
 * Fairer for members who return a book just a few days late.
 *
 * STRATEGY PATTERN:
 * This class is interchangeable with PerWeekFineCalculator.
 * LendingService holds a FineCalculator reference — it never knows which
 * concrete strategy is active. Swapping strategies requires no code change
 * in LendingService.
 */

public class PerDayFineCalculator implements IFineCalculator{
    @Override
    public double calculate(int overdueDays) {
        if (overdueDays <= 0) {
            return 0.0;
        }
        return overdueDays * AppConstants.FINE_PER_DAY;
    }
}
