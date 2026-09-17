package com.peerview.dashboard;

import com.peerview.auth.User;
import com.peerview.auth.UserRepository;
import com.peerview.evaluation.Review;
import com.peerview.evaluation.ReviewRepository;
import com.peerview.sessions.InterviewSession;
import com.peerview.sessions.InterviewSessionQueries;
import com.peerview.sessions.SessionRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class DashboardController {

    private final UserRepository users;
    private final InterviewSessionQueries sessions;
    private final ReviewRepository reviews;

    public DashboardController(UserRepository users, InterviewSessionQueries sessions, ReviewRepository reviews) {
        this.users = users;
        this.sessions = sessions;
        this.reviews = reviews;
    }

    @GetMapping("/dashboard")
    public List<DashboardSession> dashboard(Authentication authentication) {
        User user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return sessions.findDistinctByHostIdOrInterviewerIdOrIntervieweeId(user.getId(), user.getId(), user.getId()).stream()
                .map(session -> new DashboardSession(session.getId(), session.getDomain().getName(), session.getInterviewType().getName(), role(session, user), session.getStatus().name(), reviews.findBySessionId(session.getId()).map(review -> review.reportFor(role(session, user))).orElse(null), session.getCreatedAt())).toList();
    }

    private SessionRole role(InterviewSession session, User user) { return session.getInterviewer() != null && session.getInterviewer().getId().equals(user.getId()) ? SessionRole.INTERVIEWER : SessionRole.INTERVIEWEE; }
    public record DashboardSession(UUID id, String domain, String interviewType, SessionRole role, String status, String report, Instant createdAt) {}
}