package com.pravisolutions.services.notification;
/**
 * Notification channel: SMS.
 *
 * In a real system, this would call an SMS gateway API (e.g., Twilio, AWS SNS).
 * Here we simulate with System.out.println().
 *
 * SMS messages are intentionally shorter than emails — SMS has a character limit
 * in practice. This difference in message format between Email and SMS also
 * demonstrates WHY having separate implementations matters — each channel
 * has its own format, length, and tone requirements.
 *
 * memberPhone is the key field used here (vs memberEmail in EmailNotificationService).
 */
public class SmsNotificationService implements INotificationCapability{
    @Override
    public void sendOverdueNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        System.out.println("[SMS] To: " + memberPhone
                + " | Hi " + memberName + ", book '" + bookTitle
                + "' is overdue. Return ASAP to avoid fines. - Library");
    }

    @Override
    public void sendReservationAvailableNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        System.out.println("[SMS] To: " + memberPhone
                + " | Hi " + memberName + ", '" + bookTitle
                + "' is available for pickup. Collect within 2 days. - Library");
    }

    @Override
    public void sendReservationCancelledNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        System.out.println("[SMS] To: " + memberPhone
                + " | Hi " + memberName + ", your reservation for '"
                + bookTitle + "' has been cancelled. - Library");
    }
}
