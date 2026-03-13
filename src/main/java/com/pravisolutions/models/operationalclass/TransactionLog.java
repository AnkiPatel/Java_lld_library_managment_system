package com.pravisolutions.models.operationalclass;

import com.pravisolutions.enums.TransactionType;

import java.util.Date;

/**
 * Immutable audit record of every action in the library system (R1, R10).
 *
 * WHY A SEPARATE CLASS (not logging inside BookLending/Reservation):
 * R1 requires a "complete log of ALL transactions" — borrow, return,
 * reservation, renewal. If we embedded logging in each model, we'd need
 * to query multiple places to get a full picture. A unified TransactionLog
 * gives a single table/list to query for any audit need.
 *
 * WHY STORE IDs (Strings) INSTEAD OF OBJECT REFERENCES:
 * - Audit records must survive object deletion (e.g., member cancels account)
 * - Strings are serialization-friendly — easy to write to a database or file
 * - Avoids dangling references if objects are garbage collected
 *
 * R10 specifically requires: who (memberCardNumber), what (bookItemId), when (date).
 * All three are captured here. transactionType adds the "what action" dimension.
 * remarks is a free-text field for any additional context (e.g., fine amount on return).
 *
 * This class is intentionally immutable after construction — audit records
 * should never be modified after being written.
 */
public class TransactionLog {

    private final String logId;
    private final String memberCardNumber;  // WHO performed or was involved
    private final String bookItemId;        // WHAT physical copy was involved
    private final TransactionType transactionType; // WHAT action occurred
    private final Date transactionDate;     // WHEN it happened
    private final String remarks;           // Optional context (e.g., "Fine: Rs 10.0")

    public TransactionLog(String logId, String memberCardNumber,
                          String bookItemId, TransactionType transactionType,
                          Date transactionDate, String remarks) {
        this.logId             = logId;
        this.memberCardNumber  = memberCardNumber;
        this.bookItemId        = bookItemId;
        this.transactionType   = transactionType;
        this.transactionDate   = transactionDate;
        this.remarks           = remarks;
    }

    // -----------------------------------------------------------------------
    // Getters only — no setters (immutable audit record)
    // -----------------------------------------------------------------------

    public String getLogId() {
        return logId;
    }

    public String getMemberCardNumber() {
        return memberCardNumber;
    }

    public String getBookItemId() {
        return bookItemId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public String getRemarks() {
        return remarks;
    }

    @Override
    public String toString() {
        return "TransactionLog{logId='" + logId
                + "', member='" + memberCardNumber
                + "', bookItem='" + bookItemId
                + "', type=" + transactionType
                + ", date=" + transactionDate
                + ", remarks='" + remarks + "'}";
    }
}