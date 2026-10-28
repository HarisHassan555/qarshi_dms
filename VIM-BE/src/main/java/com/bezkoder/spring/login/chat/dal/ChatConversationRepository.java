package com.bezkoder.spring.login.chat.dal;

import com.bezkoder.spring.login.chat.dal.entities.ChatTblConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatTblConversation, Integer> {
}
