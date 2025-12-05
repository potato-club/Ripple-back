package org.example.rippleback.features.message.api.dto;

import org.example.rippleback.features.message.domain.MessageType;

public record MessageRequestDto(
        Long conversationId,
        String content,
        MessageType messageType
) {}