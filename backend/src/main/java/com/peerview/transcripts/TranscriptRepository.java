package com.peerview.transcripts;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {
    List<Transcript> findAllBySessionIdOrderBySequenceNo(UUID sessionId);
}