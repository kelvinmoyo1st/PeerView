package com.peerview.evaluation;

import com.peerview.auth.User;
import com.peerview.sessions.InterviewSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(nullable = false)
    private double finalOverallScore;

    @Column(nullable = false, length = 4000)
    private String interviewerNotes;

    @Column(nullable = false, length = 8000)
    private String aiNarrativeReport;

    @Column(nullable = false)
    private Instant submittedAt;

    protected Review() {}

    public Review(InterviewSession session, User interviewer, double finalOverallScore, String interviewerNotes, String aiNarrativeReport) {
        this.session = session;
        this.interviewer = interviewer;
        this.finalOverallScore = finalOverallScore;
        this.interviewerNotes = interviewerNotes;
        this.aiNarrativeReport = aiNarrativeReport;
        this.submittedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public InterviewSession getSession() { return session; }
    public double getFinalOverallScore() { return finalOverallScore; }
    public String getInterviewerNotes() { return interviewerNotes; }
    public String getAiNarrativeReport() { return aiNarrativeReport; }
    public Instant getSubmittedAt() { return submittedAt; }
}