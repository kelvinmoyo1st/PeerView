package com.peerview.evaluation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findBySessionId(UUID sessionId);
}