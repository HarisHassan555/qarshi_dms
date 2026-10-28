package com.bezkoder.spring.login.chat.dal;

import com.bezkoder.spring.login.chat.dal.entities.ChatTblConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatConversationMemberRepository extends JpaRepository<ChatTblConversationMember, Integer> {

    List<ChatTblConversationMember> findBySerUserId(Integer serUserId);

    List<ChatTblConversationMember> findBySerConversationId(Integer serConversationId);

    ChatTblConversationMember findBySerConversationIdAndSerUserId(Integer serConversationId, Integer serUserId);
}
