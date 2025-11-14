package org.example.rippleback.features.message.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.message.api.dto.MessageRequestDto;
import org.example.rippleback.features.message.domain.Conversation;
import org.example.rippleback.features.message.domain.Message;
import org.example.rippleback.features.message.infra.ConversationRepository;
import org.example.rippleback.features.message.infra.MessageRepository;
import org.example.rippleback.features.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    @Transactional
    public Message sendMessage(User sender, MessageRequestDto requestDto) {
        Conversation conversation = conversationRepository.findById(requestDto.conversationId())
                .orElseThrow(() -> new IllegalStateException("대화방을 찾을 수 없습니다."));

        if (!conversation.hasParticipant(sender)) {
            throw new IllegalStateException("대화 참여자가 아닙니다.");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(requestDto.content())
                .build();

        return messageRepository.save(message);
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findAllByConversationIdOrderBySentAtAsc(conversationId);
    }
}
