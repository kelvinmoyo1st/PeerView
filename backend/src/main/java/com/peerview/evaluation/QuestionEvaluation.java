package com.peerview.evaluation;

import com.peerview.questions.SessionQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "question_evaluations")
public class QuestionEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_question_id", nullable = false, unique = true)
    private SessionQuestion sessionQuestion;

    @Column(nullable = false)
    private int aiScore;

    @Column(nullable = false, length = 2000)
    private String aiFeedback;

    private Integer interviewerScore;

    @Column(length = 2000)
    private String interviewerFeedback;

    protected QuestionEvaluation() {}

    public QuestionEvaluation(SessionQuestion sessionQuestion, int aiScore, String aiFeedback) {
        this.sessionQuestion = sessionQuestion;
        this.aiScore = aiScore;
        this.aiFeedback = aiFeedback;
    }

    public void review(int score, String feedback) {
        if (score < 1 || score > 5) throw new IllegalArgumentException("Scores must be between 1 and 5");
        interviewerScore = score;
        interviewerFeedback = feedback;
    }

    public UUID getId() { return id; }
    public SessionQuestion getSessionQuestion() { return sessionQuestion; }
    public int getAiScore() { return aiScore; }
    public String getAiFeedback() { return aiFeedback; }
    public Integer getInterviewerScore() { return interviewerScore; }
    public String getInterviewerFeedback() { return interviewerFeedback; }
}