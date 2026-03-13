package com.pravisolutions.services.notification;

/**
 * Contract for all notification channels in the library system.
 *
 * WHY INTERFACE (Open/Closed + Composite Pattern):
 * R12 requires the system to notify members. HOW it notifies is not specified.
 * By defining this interface:
 *   - EmailNotificationService can send emails (dummy implementation here)
 *   - SmsNotificationService can send SMS (dummy implementation here)
 *   - CompositeNotificationService can delegate to BOTH simultaneously
 *
 * Adding a new channel (e.g., push notification) requires zero changes
 * to any existing service — just add a new implementation.
 *
 * All methods receive primitive data (email, phone, title) rather than
 * full objects — this keeps the notification layer decoupled from the
 * domain model and easily portable to other languages.
 */

public interface INotificationCapability {
    /**
     * Notify a member that their borrowed book is overdue.
     *
     * @param memberName  Display name of the member.
     * @param memberEmail Email address of the member.
     * @param memberPhone Phone number of the member.
     * @param bookTitle   Title of the overdue book.
     */
    void sendOverdueNotification(String memberName,
                                 String memberEmail,
                                 String memberPhone,
                                 String bookTitle);

    /**
     * Notify a member that the book they reserved is now available for pickup.
     *
     * @param memberName  Display name of the member.
     * @param memberEmail Email address of the member.
     * @param memberPhone Phone number of the member.
     * @param bookTitle   Title of the now-available reserved book.
     */
    void sendReservationAvailableNotification(String memberName,
                                              String memberEmail,
                                              String memberPhone,
                                              String bookTitle);

    /**
     * Notify a member that their reservation has been cancelled
     * (either by librarian or due to expiry).
     *
     * @param memberName  Display name of the member.
     * @param memberEmail Email address of the member.
     * @param memberPhone Phone number of the member.
     * @param bookTitle   Title of the book whose reservation was cancelled.
     */
    void sendReservationCancelledNotification(String memberName,
                                              String memberEmail,
                                              String memberPhone,
                                              String bookTitle);
}
