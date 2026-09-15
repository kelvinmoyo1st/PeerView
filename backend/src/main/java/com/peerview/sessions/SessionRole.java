package com.peerview.sessions;

public enum SessionRole {
    INTERVIEWER, INTERVIEWEE;

    public SessionRole opposite() {
        return this == INTERVIEWER ? INTERVIEWEE : INTERVIEWER;
    }
}