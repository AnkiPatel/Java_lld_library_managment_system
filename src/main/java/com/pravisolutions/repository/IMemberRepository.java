package com.pravisolutions.repository;

import com.pravisolutions.models.Member;

import java.util.List;

/**
 * Repository contract for Member storage.
 *
 * Kept separate from BookRepository because Members and Books are
 * independent aggregate roots — they have different lifecycles and
 * different access patterns. This also keeps each repository focused
 * on a single responsibility.
 */
public interface IMemberRepository {

    /** Register a new member in the system. */
    void addMember(Member member);

    /** Remove a member by their library card number. */
    void removeMember(String cardNumber);

    /**
     * Find a member by their unique library card number.
     * Returns null if not found.
     */
    Member findByCardNumber(String cardNumber);

    /** Return all registered members. */
    List<Member> getAllMembers();
}
