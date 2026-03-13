package com.pravisolutions.services;

import com.pravisolutions.enums.MemberStatus;
import com.pravisolutions.models.LibraryCard;
import com.pravisolutions.models.Member;
import com.pravisolutions.repository.IMemberRepository;

import java.util.Date;
import java.util.List;

/**
 * Handles member lifecycle: registration, updates, cancellation, card issuance.
 *
 * RESPONSIBILITY:
 * Everything related to a member's ACCOUNT — not their borrowing activity.
 * Borrowing, returning, and reserving are handled by LendingService and
 * ReservationService respectively.
 *
 * Librarian actions that map to this service:
 * - Register new member (with library card issuance)
 * - Update member details
 * - Cancel membership
 * - View member account
 */
public class MemberService {
    private IMemberRepository memberRepo;

    public MemberService(IMemberRepository repository) {
        this.memberRepo = repository;
    }

    /**
     * Register a new member and issue their library card.
     *
     * cardNumber must be globally unique (across members AND librarians).
     * In a real system, cardNumber would be auto-generated. Here it is
     * passed in to keep the code simple and testable without a UUID library.
     */
    public Member registerMember(String name, String email, String phone,
                                 String password, String cardNumber) {
        if (memberRepo.findByCardNumber(cardNumber) != null) {
            throw new IllegalArgumentException(
                    "Card number '" + cardNumber + "' is already in use.");
        }
        LibraryCard card = new LibraryCard(cardNumber, new Date());
        Member member    = new Member(name, email, phone, password, card);
        memberRepo.addMember(member);
        System.out.println("[MemberService] Member registered: " + member);
        return member;
    }

    /**
     * Update mutable fields on a member account.
     * Password update is allowed here — in production, hashing would happen first.
     */
    public void updateMember(String cardNumber, String name,
                             String email, String phone) {
        Member member = getMember(cardNumber);
        member.setName(name);
        member.setEmail(email);
        member.setPhone(phone);
        System.out.println("[MemberService] Member updated: " + member);
    }

    /**
     * Cancel a member's account.
     * Sets status to CANCELLED and deactivates their library card.
     * Does NOT delete the member — preserves audit history.
     */
    public void cancelMembership(String cardNumber) {
        Member member = getMember(cardNumber);
        member.setStatus(MemberStatus.CANCELLED);
        member.getLibraryCard().setActive(false);
        System.out.println("[MemberService] Membership cancelled: " + cardNumber);
    }

    /**
     * Find a member by card number. Throws if not found.
     * Centralised null-check used by all other services.
     */
    public Member getMember(String cardNumber) {
        Member member = memberRepo.findByCardNumber(cardNumber);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + cardNumber);
        }
        return member;
    }

    public List<Member> getAllMembers() {
        return memberRepo.getAllMembers();
    }
}
