package org.example.rippleback.features.message.api.dto;

import org.example.rippleback.features.message.domain.Message;

import java.time.LocalDateTime;

public record MessageDto(
        Long id,
        Long senderId,
        String content,
        LocalDateTime sentAt
) {
    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getSentAt()
        );
    }
}
