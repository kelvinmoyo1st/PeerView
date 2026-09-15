package com.peerview.evaluation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionEvaluationRepository extends JpaRepository<QuestionEvaluation, UUID> {
    List<QuestionEvaluation> findAllBySessionQuestionSessionIdOrderBySessionQuestionOrderIndex(UUID sessionId);
    java.util.Optional<QuestionEvaluation> findBySessionQuestionId(UUID questionId);
}