package org.example.rippleback.features.message.app;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.features.message.infra.MessageRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

}
