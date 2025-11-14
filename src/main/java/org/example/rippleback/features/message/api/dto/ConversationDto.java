package org.example.rippleback.features.message.api.dto;

import org.example.rippleback.features.message.domain.Conversation;
import org.example.rippleback.features.user.domain.User;

import java.util.Set;
import java.util.stream.Collectors;

public record ConversationDto(
        Long id,
        Set<Long> participantIds
) {
    public static ConversationDto from(Conversation conversation) {
        return new ConversationDto(
                conversation.getId(),
                conversation.getParticipants()
                        .stream().map(User::getId)
                        .collect(Collectors.toSet())
        );
    }
}
