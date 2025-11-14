package org.example.rippleback.features.message.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.message.domain.Conversation;
import org.example.rippleback.features.message.infra.ConversationRepository;
import org.example.rippleback.features.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationService {

    private final ConversationRepository conversationRepository;

    public List<Conversation> getUserConversations(User user) {
        return conversationRepository.findAllByUserId(user.getId());
    }

    @Transactional
    public Conversation createConversation(Set<User> participants) {
        Conversation conversation = Conversation.builder()
                .participants(participants)
                .build();
        return conversationRepository.save(conversation);
    }
}
