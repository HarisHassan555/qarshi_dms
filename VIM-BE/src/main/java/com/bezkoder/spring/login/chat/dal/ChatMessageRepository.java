package com.bezkoder.spring.login.chat.dal;

import com.bezkoder.spring.login.chat.dal.entities.ChatTblMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatTblMessage, Integer> {

    List<ChatTblMessage> findBySerConversationIdOrderByDteCreatedDateDesc(Integer serConversationId, Pageable pageable);

    long countBySerConversationIdAndSerMessageIdGreaterThan(Integer serConversationId, Integer serMessageId);
}
