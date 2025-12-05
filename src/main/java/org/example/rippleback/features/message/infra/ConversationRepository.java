package org.example.rippleback.features.message.infra;

import org.example.rippleback.features.message.domain.Conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation,Long> {
    @Query("SELECT c FROM Conversation c JOIN c.participants p WHERE p.id = :userId")
    List<Conversation> findAllByUserId(Long userId);

    @Query("""
        SELECT COUNT(m)
        FROM Message m
        LEFT JOIN MessageRead r
            ON r.message.id = m.id AND r.user.id = :userId
        WHERE m.conversation.id = :conversationId
          AND r.id IS NULL
    """)
    Long countUnreadMessages(Long conversationId, Long userId);
}
