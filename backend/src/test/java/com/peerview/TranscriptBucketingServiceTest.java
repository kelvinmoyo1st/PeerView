package com.peerview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.peerview.evaluation.TranscriptBucketingService;
import com.peerview.questions.SessionQuestion;
import com.peerview.transcripts.Transcript;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranscriptBucketingServiceTest {

    @Test
    void mapsTranscriptToQuestionBetweenAskedTimestamps() {
        Instant firstAsked = Instant.parse("2026-01-01T10:00:00Z");
        Instant secondAsked = Instant.parse("2026-01-01T10:05:00Z");
        SessionQuestion first = mock(SessionQuestion.class);
        SessionQuestion second = mock(SessionQuestion.class);
        Transcript answer = mock(Transcript.class);
        UUID firstId = UUID.randomUUID();
        when(first.getId()).thenReturn(firstId);
        when(first.getAskedAt()).thenReturn(firstAsked);
        when(second.getAskedAt()).thenReturn(secondAsked);
        when(answer.getSpokenAt()).thenReturn(Instant.parse("2026-01-01T10:02:00Z"));
        when(answer.getText()).thenReturn("A thoughtful answer");

        assertThat(new TranscriptBucketingService().bucket(List.of(first, second), List.of(answer)))
                .containsEntry(firstId, "A thoughtful answer");
    }
}