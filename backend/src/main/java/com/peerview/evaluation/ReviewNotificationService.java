package com.peerview.evaluation;

import com.peerview.sessions.InterviewSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ReviewNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ReviewNotificationService.class);
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;

    public ReviewNotificationService(ObjectProvider<JavaMailSender> mailSender, @Value("${peerview.mail.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.enabled = enabled;
    }

    public void send(InterviewSession session, Review review) {
        if (session.getInterviewee() == null) return;
        if (!enabled) {
            log.info("Report email disabled for interviewee {}", session.getInterviewee().getEmail());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(session.getInterviewee().getEmail());
        message.setSubject("Your PeerView interview report");
        message.setText(review.getAiNarrativeReport());
        mailSender.getObject().send(message);
    }
}