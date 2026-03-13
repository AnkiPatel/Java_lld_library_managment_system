package com.pravisolutions.services.notification;

import java.util.ArrayList;
import java.util.List;


/**
 * Composite notification channel — delegates to multiple channels simultaneously.
 *
 * COMPOSITE PATTERN:
 * CompositeNotificationService implements the SAME interface as its children.
 * The caller (LendingService, ReservationService) holds a NotificationService
 * reference — it has NO IDEA whether it's talking to Email, SMS, or both.
 *
 * This means:
 *   - To notify via Email only  → inject EmailNotificationService
 *   - To notify via SMS only    → inject SmsNotificationService
 *   - To notify via both        → inject CompositeNotificationService
 *   - To notify via 3 channels  → add a third implementation and inject here
 *
 * Zero changes needed anywhere else in the system.
 *
 * HOW TO BUILD:
 *   CompositeNotificationService notifier = new CompositeNotificationService();
 *   notifier.addChannel(new EmailNotificationService());
 *   notifier.addChannel(new SmsNotificationService());
 *
 * This is done once in Main during wiring — then passed into Library facade.
 */

public class CompositeNotificationService implements INotificationCapability{
    private List<INotificationCapability> channels;

    public CompositeNotificationService() {
        channels = new ArrayList<>();
    }

    /**
     * Add a notification channel to this composite.
     * Channels are notified in the order they are added.
     */
    public void addChannel(INotificationCapability channel) {
        channels.add(channel);
    }

    @Override
    public void sendOverdueNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        for(INotificationCapability ch : this.channels) {
            ch.sendOverdueNotification(memberName,memberEmail,memberPhone,bookTitle);
        }
    }

    @Override
    public void sendReservationAvailableNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        for(INotificationCapability ch : this.channels) {
            ch.sendReservationAvailableNotification(memberName,memberEmail,memberPhone,bookTitle);
        }
    }

    @Override
    public void sendReservationCancelledNotification(String memberName, String memberEmail, String memberPhone, String bookTitle) {
        for(INotificationCapability ch : this.channels) {
            ch.sendReservationCancelledNotification(memberName,memberEmail,memberPhone,bookTitle);
        }
    }
}
