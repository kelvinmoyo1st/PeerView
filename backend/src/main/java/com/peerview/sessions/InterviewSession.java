package com.peerview.sessions;

import com.peerview.auth.User;
import com.peerview.catalog.Domain;
import com.peerview.catalog.InterviewType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_type_id", nullable = false)
    private InterviewType interviewType;

    @Column(name = "invite_token", nullable = false, unique = true, length = 64)
    private String inviteToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    private User interviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewee_id")
    private User interviewee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant startedAt;
    private Instant endedAt;

    protected InterviewSession() {}

    public InterviewSession(User host, Domain domain, InterviewType interviewType, String inviteToken, SessionRole hostRole) {
        this.host = host;
        this.domain = domain;
        this.interviewType = interviewType;
        this.inviteToken = inviteToken;
        this.status = SessionStatus.INVITED;
        this.createdAt = Instant.now();
        assign(host, hostRole);
    }

    public void join(User peer, SessionRole role) {
        if (status != SessionStatus.INVITED || interviewer != null && interviewee != null) {
            throw new IllegalStateException("This invite is no longer available");
        }
        assign(peer, role);
        status = SessionStatus.JOINED;
    }

    public void start() {
        if (status != SessionStatus.JOINED) throw new IllegalStateException("Both participants must join first");
        status = SessionStatus.IN_PROGRESS;
        startedAt = Instant.now();
    }

    public void end() {
        if (status != SessionStatus.IN_PROGRESS && status != SessionStatus.JOINED) {
            throw new IllegalStateException("This session is not active");
        }
        status = SessionStatus.COMPLETED;
        endedAt = Instant.now();
    }

    public void reviewed() {
        if (status != SessionStatus.COMPLETED) throw new IllegalStateException("The session must be completed first");
        status = SessionStatus.REVIEWED;
    }

    private void assign(User participant, SessionRole role) {
        if (role == SessionRole.INTERVIEWER) interviewer = participant;
        else interviewee = participant;
    }

    public UUID getId() { return id; }
    public User getHost() { return host; }
    public Domain getDomain() { return domain; }
    public InterviewType getInterviewType() { return interviewType; }
    public String getInviteToken() { return inviteToken; }
    public User getInterviewer() { return interviewer; }
    public User getInterviewee() { return interviewee; }
    public SessionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}