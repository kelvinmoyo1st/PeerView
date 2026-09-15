package com.peerview.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interview_types")
public class InterviewType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @OneToMany(mappedBy = "interviewType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<InterviewSection> sections = new ArrayList<>();

    protected InterviewType() {}

    public InterviewType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void addSection(String name, int orderIndex, int durationMinutes) {
        sections.add(new InterviewSection(this, name, orderIndex, durationMinutes));
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<InterviewSection> getSections() { return sections; }
}