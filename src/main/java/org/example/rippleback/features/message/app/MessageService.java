package org.example.rippleback.features.message.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.core.error.BusinessException;
import org.example.rippleback.core.error.ErrorCode;
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
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public Message sendMessage(User sender, MessageRequestDto requestDto) {
        Conversation conversation = conversationRepository.findById(requestDto.conversationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONVERSATION));

        if (!conversation.hasParticipant(sender)) {
            throw new BusinessException(ErrorCode.NOT_CONTAIN_CONVERSATION);
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(requestDto.content())
                .messageType(requestDto.messageType())
                .build();

        return messageRepository.save(message);
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findAllByConversationIdOrderBySentAtAsc(conversationId);
    }
}
