package com.peerview.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class InviteNotificationService {

    private static final Logger log = LoggerFactory.getLogger(InviteNotificationService.class);
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String frontendUrl;

    public InviteNotificationService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${peerview.mail.enabled:false}") boolean enabled,
            @Value("${peerview.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.frontendUrl = frontendUrl;
    }

    public String inviteUrl(String token) {
        return frontendUrl.replaceAll("/$", "") + "/join/" + token;
    }

    public void send(String email, String hostName, String token) {
        String url = inviteUrl(token);
        if (!enabled) {
            log.info("Invite email disabled; invite URL for {} is {}", email, url);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(hostName + " invited you to a PeerView interview");
        message.setText("Join the interview here: " + url);
        mailSender.getObject().send(message);
    }
}