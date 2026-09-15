package com.peerview.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "interview_sections")
public class InterviewSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_type_id", nullable = false)
    private InterviewType interviewType;

    @Column(nullable = false)
    private String name;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    protected InterviewSection() {}

    InterviewSection(InterviewType interviewType, String name, int orderIndex, int durationMinutes) {
        this.interviewType = interviewType;
        this.name = name;
        this.orderIndex = orderIndex;
        this.durationMinutes = durationMinutes;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getOrderIndex() { return orderIndex; }
    public int getDurationMinutes() { return durationMinutes; }
}