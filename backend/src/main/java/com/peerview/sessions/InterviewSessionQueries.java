package com.peerview.sessions;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionQueries extends JpaRepository<InterviewSession, UUID> {
    List<InterviewSession> findDistinctByHostIdOrInterviewerIdOrIntervieweeId(UUID hostId, UUID interviewerId, UUID intervieweeId);
}