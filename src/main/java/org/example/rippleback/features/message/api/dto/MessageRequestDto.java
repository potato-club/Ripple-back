package org.example.rippleback.features.message.api.dto;

public record MessageRequestDto(
        Long conversationId,
        String content
) {}