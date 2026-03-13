package com.pravisolutions.services.notification;
/**
 * Notification channel: Email.
 *
 * In a real system, this class would integrate with an email provider
 * (e.g., SendGrid, JavaMail). Here we simulate the behaviour with
 * System.out.println() to keep the code framework-free and portable.
 *
 * The method signatures, parameter names, and print format are designed
 * to show exactly what data an email would carry — so an interviewer can
 * clearly see what the real implementation would do.
 *
 * WHY memberName is included:
 * A real email would address the member by name ("Dear Anki, ...").
 * Passing it here makes the dummy output realistic.
 */
public class EmailNotificationService implements INotificationCapability{
    @Override
    public void sendOverdueNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        System.out.println("[EMAIL] To: " + memberEmail
                + " | Dear " + memberName + ", your borrowed book '"
                + bookTitle + "' is overdue. Please return it to avoid further fines.");
    }

    @Override
    public void sendReservationAvailableNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        System.out.println("[EMAIL] To: " + memberEmail
                + " | Dear " + memberName + ", the book '"
                + bookTitle + "' you reserved is now available for pickup. "
                + "Please collect it within 2 days or your reservation will expire.");
    }

    @Override
    public void sendReservationCancelledNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        System.out.println("[EMAIL] To: " + memberEmail
                + " | Dear " + memberName + ", your reservation for '"
                + bookTitle + "' has been cancelled.");
    }
}
