package com.peerview.transcripts;

import com.peerview.auth.User;
import com.peerview.auth.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/sessions/{sessionId}/transcript")
public class TranscriptController {

    private final TranscriptService service;
    private final UserRepository users;

    public TranscriptController(TranscriptService service, UserRepository users) { this.service = service; this.users = users; }

    @PostMapping
    public TranscriptService.TranscriptResponse append(@PathVariable UUID sessionId, @Valid @RequestBody AppendRequest request, Authentication authentication) {
        return response(service.append(sessionId, user(authentication), request.text(), request.sectionName()));
    }

    @GetMapping
    public List<TranscriptService.TranscriptResponse> list(@PathVariable UUID sessionId, Authentication authentication) { return service.list(sessionId, user(authentication)); }

    private User user(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("User not found")); }
    private TranscriptService.TranscriptResponse response(Transcript transcript) { return new TranscriptService.TranscriptResponse(transcript.getId(), transcript.getText(), transcript.getSectionName(), transcript.getSpokenAt(), transcript.getSequenceNo()); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(IllegalArgumentException exception) { return new ErrorResponse(exception.getMessage(), Instant.now()); }

    public record AppendRequest(@NotBlank String text, String sectionName) {}
    public record ErrorResponse(String message, Instant timestamp) {}
}