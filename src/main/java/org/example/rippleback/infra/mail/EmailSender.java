package org.example.rippleback.infra.mail;

public interface EmailSender {
    void send(String to, String subject, String text);
}