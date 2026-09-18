package com.peerview.dashboard;

import com.peerview.auth.User;
import com.peerview.auth.UserRepository;
import com.peerview.evaluation.Review;
import com.peerview.evaluation.ReviewRepository;
import com.peerview.sessions.InterviewSession;
import com.peerview.sessions.InterviewSessionQueries;
import com.peerview.sessions.SessionRole;
import com.peerview.peers.PeerRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/users/me")
public class DashboardController {

    private final UserRepository users;
    private final InterviewSessionQueries sessions;
    private final ReviewRepository reviews;
    private final PeerRepository peers;

    public DashboardController(UserRepository users, InterviewSessionQueries sessions, ReviewRepository reviews, PeerRepository peers) {
        this.users = users;
        this.sessions = sessions;
        this.reviews = reviews;
        this.peers = peers;
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public List<DashboardSession> dashboard(Authentication authentication) {
        User user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return sessions.findDistinctByHostIdOrInterviewerIdOrIntervieweeId(user.getId(), user.getId(), user.getId()).stream()
                .map(session -> new DashboardSession(session.getId(), session.getDomain().getName(), session.getInterviewType().getName(), role(session, user), session.getStatus().name(), peerName(session, user), reviews.findBySessionId(session.getId()).map(review -> review.reportFor(role(session, user))).orElse(null), session.getCreatedAt(), peers.countByUserId(user.getId()))).toList();
    }

    private SessionRole role(InterviewSession session, User user) { return session.getInterviewer() != null && session.getInterviewer().getId().equals(user.getId()) ? SessionRole.INTERVIEWER : SessionRole.INTERVIEWEE; }
    private String peerName(InterviewSession session, User user) {
        SessionRole userRole = role(session, user);
        User peer = userRole == SessionRole.INTERVIEWER ? session.getInterviewee() : session.getInterviewer();
        return peer == null ? null : peer.getName();
    }
    public record DashboardSession(UUID id, String domain, String interviewType, SessionRole role, String status, String peerName, String report, Instant createdAt, long peerCount) {}
}