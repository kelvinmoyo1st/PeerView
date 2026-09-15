package com.peerview.questions;

import com.peerview.auth.User;
import com.peerview.sessions.InterviewSession;
import com.peerview.sessions.InterviewSessionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionService {

    private final SessionQuestionRepository questions;
    private final InterviewSessionRepository sessions;
    private final GeminiQuestionGenerator generator;

    public QuestionService(SessionQuestionRepository questions, InterviewSessionRepository sessions, GeminiQuestionGenerator generator) {
        this.questions = questions;
        this.sessions = sessions;
        this.generator = generator;
    }

    @Transactional
    public void generateFor(InterviewSession session) {
        if (questions.findAllBySessionIdOrderByOrderIndex(session.getId()).isEmpty()) {
            questions.saveAll(generator.generate(session).stream().map(item -> new SessionQuestion(
                    session, item.sectionName(), item.orderIndex(), item.text(), item.keyPoints())).toList());
        }
    }

    @Transactional(readOnly = true)
    public List<SessionQuestion> forInterviewer(UUID sessionId, User user) {
        InterviewSession session = session(sessionId);
        requireInterviewer(session, user);
        return questions.findAllBySessionIdOrderByOrderIndex(sessionId);
    }

    @Transactional
    public SessionQuestion markAsked(UUID sessionId, UUID questionId, User user) {
        InterviewSession session = session(sessionId);
        requireInterviewer(session, user);
        SessionQuestion question = questions.findByIdAndSessionId(questionId, sessionId).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        question.markAsked();
        return question;
    }

    private InterviewSession session(UUID id) { return sessions.findById(id).orElseThrow(() -> new IllegalArgumentException("Session not found")); }
    private void requireInterviewer(InterviewSession session, User user) {
        if (session.getInterviewer() == null || !session.getInterviewer().getId().equals(user.getId())) throw new IllegalArgumentException("Only the interviewer can access questions");
    }
}