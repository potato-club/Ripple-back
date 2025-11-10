package org.example.rippleback.features.message.infra;

import org.example.rippleback.features.message.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation,Long> {
    Optional<Conversation> findByUserAIdAndUserBId(Long userAId, Long userBId);
    Optional<Conversation> findByUserBIdAndUserAId(Long userBId, Long userAId);

}
