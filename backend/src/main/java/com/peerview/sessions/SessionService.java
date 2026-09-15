package com.peerview.sessions;

import com.peerview.auth.User;
import com.peerview.auth.UserRepository;
import com.peerview.catalog.Domain;
import com.peerview.catalog.DomainRepository;
import com.peerview.catalog.InterviewType;
import com.peerview.catalog.InterviewTypeRepository;
import com.peerview.questions.QuestionService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final InterviewSessionRepository sessions;
    private final UserRepository users;
    private final DomainRepository domains;
    private final InterviewTypeRepository types;
    private final InviteNotificationService notifications;
    private final QuestionService questionService;

    public SessionService(InterviewSessionRepository sessions, UserRepository users, DomainRepository domains,
                          InterviewTypeRepository types, InviteNotificationService notifications, QuestionService questionService) {
        this.sessions = sessions;
        this.users = users;
        this.domains = domains;
        this.types = types;
        this.notifications = notifications;
        this.questionService = questionService;
    }

    @Transactional
    public InterviewSession create(User host, UUID domainId, UUID typeId, SessionRole hostRole, String peerEmail) {
        Domain domain = domains.findById(domainId).orElseThrow(() -> new IllegalArgumentException("Domain not found"));
        InterviewType type = types.findById(typeId).orElseThrow(() -> new IllegalArgumentException("Interview type not found"));
        String token = UUID.randomUUID().toString().replace("-", "");
        InterviewSession session = sessions.save(new InterviewSession(host, domain, type, token, hostRole));
        questionService.generateFor(session);
        notifications.send(peerEmail.trim().toLowerCase(), host.getName(), token);
        return session;
    }

    @Transactional
    public InterviewSession join(String token, String name, String email) {
        InterviewSession session = sessions.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found"));
        User peer = users.findByEmailIgnoreCase(email.trim()).orElseGet(() -> users.save(new User(name.trim(), email.trim().toLowerCase(), null)));
        SessionRole peerRole = session.getInterviewer() == null ? SessionRole.INTERVIEWER : SessionRole.INTERVIEWEE;
        session.join(peer, peerRole);
        return session;
    }

    @Transactional(readOnly = true)
    public SessionController.InviteResponse invite(String token) {
        InterviewSession session = sessions.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found or expired"));
        return new SessionController.InviteResponse(session.getDomain().getName(), session.getInterviewType().getName(), session.getStatus());
    }
}