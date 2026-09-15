package com.peerview.evaluation;

import com.peerview.questions.SessionQuestion;
import com.peerview.transcripts.Transcript;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TranscriptBucketingService {

    public Map<UUID, String> bucket(List<SessionQuestion> questions, List<Transcript> transcripts) {
        return questions.stream().collect(Collectors.toMap(SessionQuestion::getId, question -> transcripts.stream()
                .filter(transcript -> isAfter(transcript.getSpokenAt(), question.getAskedAt()))
                .filter(transcript -> nextQuestionTime(questions, question).map(next -> transcript.getSpokenAt().isBefore(next)).orElse(true))
                .map(Transcript::getText).collect(Collectors.joining(" "))));
    }

    private boolean isAfter(Instant transcriptTime, Instant askedAt) { return askedAt != null && !transcriptTime.isBefore(askedAt); }

    private java.util.Optional<Instant> nextQuestionTime(List<SessionQuestion> questions, SessionQuestion current) {
        return questions.stream().map(SessionQuestion::getAskedAt).filter(time -> time != null && current.getAskedAt() != null && time.isAfter(current.getAskedAt())).min(Instant::compareTo);
    }
}