package org.example.rippleback.features.message.api;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.message.api.dto.MessageDto;
import org.example.rippleback.features.message.api.dto.MessageRequestDto;
import org.example.rippleback.features.message.app.MessageService;
import org.example.rippleback.features.message.domain.Message;
import org.example.rippleback.features.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
public class MessageController  {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageDto> sendMessage(
            @AuthenticationPrincipal User sender,
            @RequestBody MessageRequestDto request
    ) {
        Message message = messageService.sendMessage(sender, request);
        return ResponseEntity.ok(MessageDto.from(message));
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long conversationId) {
        List<MessageDto> messages = messageService.getMessages(conversationId)
                .stream().map(MessageDto::from).toList();
        return ResponseEntity.ok(messages);
    }
}
