package com.peerview.transcripts;

import com.peerview.auth.User;
import com.peerview.sessions.InterviewSession;
import com.peerview.sessions.InterviewSessionRepository;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscriptService {

    private final TranscriptRepository transcripts;
    private final InterviewSessionRepository sessions;
    private final SimpMessagingTemplate messaging;

    public TranscriptService(TranscriptRepository transcripts, InterviewSessionRepository sessions, SimpMessagingTemplate messaging) {
        this.transcripts = transcripts;
        this.sessions = sessions;
        this.messaging = messaging;
    }

    @Transactional
    public Transcript append(UUID sessionId, User speaker, String text, String sectionName) {
        InterviewSession session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (session.getInterviewee() == null || !session.getInterviewee().getId().equals(speaker.getId())) {
            throw new IllegalArgumentException("Only the interviewee can submit transcript text");
        }
        long sequence = transcripts.count() + 1;
        Transcript transcript = transcripts.save(new Transcript(session, text.trim(), sectionName, sequence));
        messaging.convertAndSend("/topic/session/" + sessionId + "/transcript", new TranscriptResponse(
                transcript.getId(), transcript.getText(), transcript.getSectionName(), transcript.getSpokenAt(), transcript.getSequenceNo()));
        return transcript;
    }

    @Transactional(readOnly = true)
    public java.util.List<TranscriptResponse> list(UUID sessionId, User user) {
        InterviewSession session = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (!isParticipant(session, user)) throw new IllegalArgumentException("You are not part of this session");
        return transcripts.findAllBySessionIdOrderBySequenceNo(sessionId).stream()
                .map(item -> new TranscriptResponse(item.getId(), item.getText(), item.getSectionName(), item.getSpokenAt(), item.getSequenceNo())).toList();
    }

    private boolean isParticipant(InterviewSession session, User user) {
        return session.getInterviewer() != null && session.getInterviewer().getId().equals(user.getId())
                || session.getInterviewee() != null && session.getInterviewee().getId().equals(user.getId());
    }

    public record TranscriptResponse(UUID id, String text, String sectionName, java.time.Instant spokenAt, long sequenceNo) {}
}