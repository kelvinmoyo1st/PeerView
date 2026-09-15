package com.peerview.transcripts;

import com.peerview.sessions.InterviewSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transcripts")
public class Transcript {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(nullable = false, length = 4000)
    private String text;

    @Column(name = "section_name")
    private String sectionName;

    @Column(nullable = false)
    private Instant spokenAt;

    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;

    protected Transcript() {}

    public Transcript(InterviewSession session, String text, String sectionName, long sequenceNo) {
        this.session = session;
        this.text = text;
        this.sectionName = sectionName;
        this.spokenAt = Instant.now();
        this.sequenceNo = sequenceNo;
    }

    public UUID getId() { return id; }
    public String getText() { return text; }
    public String getSectionName() { return sectionName; }
    public Instant getSpokenAt() { return spokenAt; }
    public long getSequenceNo() { return sequenceNo; }
}