package com.peerview.evaluation;

import com.peerview.auth.User;
import com.peerview.auth.UserRepository;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
public class EvaluationController {

    private final EvaluationService service;
    private final UserRepository users;

    public EvaluationController(EvaluationService service, UserRepository users) { this.service = service; this.users = users; }

    @PostMapping("/end")
    public void end(@PathVariable UUID sessionId, Authentication authentication) { service.end(sessionId, user(authentication)); }

    @GetMapping("/evaluations")
    public List<EvaluationService.EvaluationResponse> evaluations(@PathVariable UUID sessionId, Authentication authentication) { return service.evaluations(sessionId, user(authentication)); }

    @PostMapping("/review")
    public ReviewResponse review(@PathVariable UUID sessionId, @Valid @RequestBody ReviewRequest request, Authentication authentication) {
        Review review = service.review(sessionId, user(authentication), request.items(), request.notes());
        return new ReviewResponse(review.getId(), review.getFinalOverallScore(), review.getAiNarrativeReport(), review.getSubmittedAt());
    }

    private User user(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("User not found")); }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(RuntimeException exception) { return new ErrorResponse(exception.getMessage(), Instant.now()); }

    public record ReviewRequest(@NotNull List<EvaluationService.ReviewItem> items, String notes) {}
    public record ReviewResponse(UUID id, double finalOverallScore, String narrative, Instant submittedAt) {}
    public record ErrorResponse(String message, Instant timestamp) {}
}