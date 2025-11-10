package org.example.rippleback.features.message.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDto {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime sendAt;

}
