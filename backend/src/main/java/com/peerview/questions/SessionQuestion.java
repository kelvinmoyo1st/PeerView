package com.peerview.questions;

import com.peerview.sessions.InterviewSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "session_questions")
public class SessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(nullable = false)
    private String sectionName;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false, length = 2000)
    private String keyPoints;

    private Instant askedAt;

    protected SessionQuestion() {}

    public SessionQuestion(InterviewSession session, String sectionName, int orderIndex, String text, String keyPoints) {
        this.session = session;
        this.sectionName = sectionName;
        this.orderIndex = orderIndex;
        this.text = text;
        this.keyPoints = keyPoints;
    }

    public void markAsked() { askedAt = Instant.now(); }
    public UUID getId() { return id; }
    public InterviewSession getSession() { return session; }
    public String getSectionName() { return sectionName; }
    public int getOrderIndex() { return orderIndex; }
    public String getText() { return text; }
    public String getKeyPoints() { return keyPoints; }
    public Instant getAskedAt() { return askedAt; }
}