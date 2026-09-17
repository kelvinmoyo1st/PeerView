package com.peerview.evaluation;

import com.peerview.auth.User;
import com.peerview.questions.SessionQuestion;
import com.peerview.questions.SessionQuestionRepository;
import com.peerview.sessions.InterviewSession;
import com.peerview.sessions.InterviewSessionRepository;
import com.peerview.transcripts.Transcript;
import com.peerview.transcripts.TranscriptRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService {

    private final InterviewSessionRepository sessions;
    private final SessionQuestionRepository questions;
    private final TranscriptRepository transcripts;
    private final QuestionEvaluationRepository evaluations;
    private final ReviewRepository reviews;
    private final TranscriptBucketingService bucketing;
    private final ReviewNotificationService notifications;
    private final SimpMessagingTemplate messaging;

    public EvaluationService(InterviewSessionRepository sessions, SessionQuestionRepository questions, TranscriptRepository transcripts,
                              QuestionEvaluationRepository evaluations, ReviewRepository reviews, TranscriptBucketingService bucketing,
                              ReviewNotificationService notifications, SimpMessagingTemplate messaging) {
        this.sessions = sessions;
        this.questions = questions;
        this.transcripts = transcripts;
        this.evaluations = evaluations;
        this.reviews = reviews;
        this.bucketing = bucketing;
        this.notifications = notifications;
        this.messaging = messaging;
    }

    @Transactional
    public InterviewSession end(UUID sessionId, User participant) {
        InterviewSession session = session(sessionId);
        requireParticipant(session, participant);
        if (session.getStatus() == com.peerview.sessions.SessionStatus.COMPLETED
                || session.getStatus() == com.peerview.sessions.SessionStatus.REVIEWED) {
            return session;
        }
        session.end();
        List<SessionQuestion> sessionQuestions = questions.findAllBySessionIdOrderByOrderIndex(sessionId);
        var answers = bucketing.bucket(sessionQuestions, transcripts.findAllBySessionIdOrderBySequenceNo(sessionId));
        evaluations.saveAll(sessionQuestions.stream().map(question -> new QuestionEvaluation(question, score(answers.get(question.getId())), feedback(answers.get(question.getId())))).toList());
        messaging.convertAndSend("/topic/session/" + sessionId + "/signal", new SessionEndedMessage("ended", java.util.Map.of("endedAt", System.currentTimeMillis())));
        return session;
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> evaluations(UUID sessionId, User interviewer) {
        InterviewSession session = session(sessionId);
        requireInterviewer(session, interviewer);
        return evaluations.findAllBySessionQuestionSessionIdOrderBySessionQuestionOrderIndex(sessionId).stream().map(item -> new EvaluationResponse(
                item.getSessionQuestion().getId(), item.getSessionQuestion().getText(), item.getAiScore(), item.getAiFeedback(), item.getInterviewerScore(), item.getInterviewerFeedback())).toList();
    }

    @Transactional
    public Review review(UUID sessionId, User interviewer, List<ReviewItem> items, String notes) {
        InterviewSession session = session(sessionId);
        requireInterviewer(session, interviewer);
        if (session.getStatus() != com.peerview.sessions.SessionStatus.COMPLETED) throw new IllegalArgumentException("End the call before submitting a review");
        items.forEach(item -> evaluations.findBySessionQuestionId(item.questionId()).orElseThrow(() -> new IllegalArgumentException("Evaluation not found")).review(item.score(), item.feedback()));
        List<QuestionEvaluation> finalEvaluations = evaluations.findAllBySessionQuestionSessionIdOrderBySessionQuestionOrderIndex(sessionId);
        if (finalEvaluations.stream().anyMatch(item -> item.getInterviewerScore() == null)) throw new IllegalArgumentException("Every question needs an interviewer score");
        double overall = finalEvaluations.stream().mapToInt(QuestionEvaluation::getInterviewerScore).average().orElse(0);
        Review review = reviews.save(new Review(session, interviewer, overall, notes == null ? "" : notes, narrative(overall, finalEvaluations, notes)));
        session.reviewed();
        notifications.send(session, review);
        return review;
    }

    private int score(String answer) { return answer == null || answer.isBlank() ? 1 : Math.min(5, 3 + Math.min(2, answer.trim().split("\\s+").length / 40)); }
    private String feedback(String answer) { return answer == null || answer.isBlank() ? "No transcript was captured for this question." : "Transcript captured: " + answer.trim(); }
    private String narrative(double overall, List<QuestionEvaluation> items, String notes) {
        long answered = items.stream().filter(item -> item.getAiScore() > 1).count();
        String noteText = notes == null || notes.isBlank() ? "No additional interviewer notes were provided." : "Interviewer notes: " + notes.trim();
        return "Recorded report: " + answered + " of " + items.size() + " questions had transcript evidence. "
                + "The displayed score is the interviewer's average of the question scores, not an inferred claim about unrecorded answers. "
                + "Average score: " + String.format("%.1f", overall) + "/5. " + noteText;
    }
    private InterviewSession session(UUID id) { return sessions.findById(id).orElseThrow(() -> new IllegalArgumentException("Session not found")); }
    private void requireParticipant(InterviewSession session, User user) { if (!isParticipant(session, user)) throw new IllegalArgumentException("You are not part of this session"); }
    private void requireInterviewer(InterviewSession session, User user) { if (session.getInterviewer() == null || !session.getInterviewer().getId().equals(user.getId())) throw new IllegalArgumentException("Only the interviewer can review this session"); }
    private boolean isParticipant(InterviewSession session, User user) { return session.getInterviewer() != null && session.getInterviewer().getId().equals(user.getId()) || session.getInterviewee() != null && session.getInterviewee().getId().equals(user.getId()); }

    public record EvaluationResponse(UUID questionId, String question, int aiScore, String aiFeedback, Integer interviewerScore, String interviewerFeedback) {}
    public record ReviewItem(UUID questionId, int score, String feedback) {}
    public record SessionEndedMessage(String type, java.util.Map<String, Object> payload) {}
}