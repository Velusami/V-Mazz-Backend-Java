package com.example.catalog.vmazz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RegistrationEmailService {
    private final ObjectProvider<JavaMailSender> mailSender;
    @Value("${spring.mail.username:}") private String from;
    @Value("${vmazz.email-notifications.enabled:false}") private boolean enabled;

    public RegistrationEmailService(ObjectProvider<JavaMailSender> mailSender) { this.mailSender = mailSender; }

    public void sendWelcome(String to, String name, String accountType) {
        if (!enabled) {
            log.info("Registration email skipped (SMTP notifications disabled) for {}", to);
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Registration email not sent: JavaMailSender is not configured");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) message.setFrom(from);
            message.setTo(to);
            message.setSubject("Welcome to V-Mazz");
            message.setText("Hello " + name + ",\n\nYour V-Mazz " + accountType + " account has been created successfully. You can now sign in and start using the marketplace.\n\nRegards,\nV-Mazz Team");
            sender.send(message);
        } catch (Exception ex) {
            // Do not roll back a successful registration because an external SMTP server is unavailable.
            log.error("Could not send V-Mazz registration email to {}", to, ex);
        }
    }
}
