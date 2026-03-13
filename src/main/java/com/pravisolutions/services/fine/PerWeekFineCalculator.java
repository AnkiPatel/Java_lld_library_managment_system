package com.pravisolutions.services.fine;

import com.pravisolutions.constants.AppConstants;

/**
 * Fine strategy: charge Rs 10.0 per overdue week (or part thereof).
 *
 * "Part thereof" means even 1 day into a new week counts as a full week.
 * This is achieved using Math.ceil() on the division.
 *
 * Examples:
 *   overdueDays = 7  → ceil(7/7)  = 1 week  → fine = 1 * 10.0  = Rs 10.0
 *   overdueDays = 8  → ceil(8/7)  = 2 weeks → fine = 2 * 10.0  = Rs 20.0
 *   overdueDays = 14 → ceil(14/7) = 2 weeks → fine = 2 * 10.0  = Rs 20.0
 *   overdueDays = 1  → ceil(1/7)  = 1 week  → fine = 1 * 10.0  = Rs 10.0
 *   overdueDays = 0  → fine = Rs 0.0
 *
 * NOTE: Math.ceil() returns a double. We cast to int before multiplying
 * to keep arithmetic clean and avoid floating point accumulation.
 */

public class PerWeekFineCalculator implements IFineCalculator{

    @Override
    public double calculate(int overdueDays) {
        if (overdueDays <= 0) {
            return 0.0;
        }
        int weeksOverdue = (int) Math.ceil((double) overdueDays / 7);
        return weeksOverdue * AppConstants.FINE_PER_WEEK;
    }
}
