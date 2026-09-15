package com.peerview.questions;

import com.peerview.auth.User;
import com.peerview.auth.UserRepository;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/questions")
public class QuestionController {

    private final QuestionService service;
    private final UserRepository users;

    public QuestionController(QuestionService service, UserRepository users) { this.service = service; this.users = users; }

    @GetMapping
    public List<QuestionResponse> questions(@PathVariable UUID sessionId, Authentication authentication) {
        return service.forInterviewer(sessionId, user(authentication)).stream().map(QuestionResponse::from).toList();
    }

    @PostMapping("/{questionId}/mark-asked")
    public QuestionResponse markAsked(@PathVariable UUID sessionId, @PathVariable UUID questionId, Authentication authentication) {
        return QuestionResponse.from(service.markAsked(sessionId, questionId, user(authentication)));
    }

    private User user(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("User not found")); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(IllegalArgumentException exception) { return new ErrorResponse(exception.getMessage(), Instant.now()); }

    public record QuestionResponse(UUID id, String sectionName, int orderIndex, String text, Instant askedAt) {
        static QuestionResponse from(SessionQuestion question) { return new QuestionResponse(question.getId(), question.getSectionName(), question.getOrderIndex(), question.getText(), question.getAskedAt()); }
    }
    public record ErrorResponse(String message, Instant timestamp) {}
}