package org.example.rippleback.infra.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class JavaMailEmailSenderAdapter implements EmailSender {
    private final JavaMailSender mailSender;
    @Value("${app.mail.from:no-reply@ripple.dev}") private String from;

    @Override
    public void send(String to, String subject, String text) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(to);
        m.setSubject(subject);
        m.setText(text);
        mailSender.send(m);
    }
}
