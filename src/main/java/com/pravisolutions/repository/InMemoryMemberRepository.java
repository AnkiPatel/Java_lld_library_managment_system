package com.pravisolutions.repository;

import com.pravisolutions.models.Member;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory implementation of IMemberRepository.
 *
 * cardNumber is the primary key — it is unique across all users (R6)
 * and is what gets recorded in every transaction log (R10).
 *
 * Members are never hard-deleted from the map — in a real system,
 * we would use soft delete (status = CANCELLED) to preserve audit history.
 * removeMember() here physically removes for simplicity, but the
 * TransactionLog still holds the card number strings as permanent records.
 */
public class InMemoryMemberRepository implements IMemberRepository {

    private Map<String, Member> memberMap;

    public InMemoryMemberRepository() {
        memberMap = new HashMap<>();
    }

    @Override
    public void addMember(Member member) {
        memberMap.put(member.getLibraryCard().getCardNumber(), member);
    }

    @Override
    public void removeMember(String cardNumber) {
        memberMap.remove(cardNumber);
    }

    @Override
    public Member findByCardNumber(String cardNumber) {
        return memberMap.get(cardNumber);
    }

    @Override
    public List<Member> getAllMembers() {
        return new ArrayList<>(this.memberMap.values());
    }
}
