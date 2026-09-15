package com.peerview.sessions;

import com.peerview.auth.User;
import com.peerview.auth.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
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
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;
    private final UserRepository users;
    private final InviteNotificationService notifications;

    public SessionController(SessionService service, UserRepository users, InviteNotificationService notifications) {
        this.service = service;
        this.users = users;
        this.notifications = notifications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(Authentication authentication, @Valid @RequestBody CreateSessionRequest request) {
        User host = user(authentication);
        return SessionResponse.from(service.create(host, request.domainId(), request.interviewTypeId(), request.hostRole(), request.peerEmail()), notifications, host.getEmail());
    }

    @PostMapping("/{token}/join")
    public SessionResponse join(@PathVariable String token, @Valid @RequestBody JoinRequest request) {
        return SessionResponse.from(service.join(token, request.name(), request.email()), notifications, request.email());
    }

    @GetMapping("/invite/{token}")
    public InviteResponse invite(@PathVariable String token) {
        return service.invite(token);
    }

    private User user(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(RuntimeException exception) {
        return new ErrorResponse(exception.getMessage(), Instant.now());
    }

    public record CreateSessionRequest(@NotNull UUID domainId, @NotNull UUID interviewTypeId, @NotNull SessionRole hostRole,
                                       @NotBlank @Email String peerEmail) {}
    public record JoinRequest(@NotBlank String name, @NotBlank @Email String email) {}
    public record InviteResponse(String domain, String interviewType, SessionStatus status) {}
    public record SessionResponse(UUID id, String domain, String interviewType, SessionStatus status, SessionRole role, String inviteUrl, List<SectionResponse> sections) {
        static SessionResponse from(InterviewSession session, InviteNotificationService notifications, String viewerEmail) {
            SessionRole role = session.getInterviewer() != null && session.getInterviewer().getEmail().equalsIgnoreCase(viewerEmail)
                ? SessionRole.INTERVIEWER : SessionRole.INTERVIEWEE;
            List<SectionResponse> sections = session.getInterviewType().getSections().stream().map(section -> new SectionResponse(section.getName(), section.getDurationMinutes())).toList();
            return new SessionResponse(session.getId(), session.getDomain().getName(), session.getInterviewType().getName(), session.getStatus(), role, notifications.inviteUrl(session.getInviteToken()), sections);
        }
    }
    public record SectionResponse(String name, int durationMinutes) {}
    public record ErrorResponse(String message, Instant timestamp) {}
}