package org.example.rippleback.features.message.infra;

import org.example.rippleback.features.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findAllByConversationIdOrderBySentAtAsc(Long conversationId);
}
