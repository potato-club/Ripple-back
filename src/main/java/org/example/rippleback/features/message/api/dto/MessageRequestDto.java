package org.example.rippleback.features.message.api.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequestDto {
    private Long receiverId;
    private String content;
}