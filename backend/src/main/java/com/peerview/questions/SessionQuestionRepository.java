package com.peerview.questions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionQuestionRepository extends JpaRepository<SessionQuestion, UUID> {
    List<SessionQuestion> findAllBySessionIdOrderByOrderIndex(UUID sessionId);
    Optional<SessionQuestion> findByIdAndSessionId(UUID id, UUID sessionId);
}