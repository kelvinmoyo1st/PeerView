package com.peerview.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final DomainRepository domains;
    private final InterviewTypeRepository types;

    public CatalogController(DomainRepository domains, InterviewTypeRepository types) {
        this.domains = domains;
        this.types = types;
    }

    @GetMapping("/domains")
    public List<DomainResponse> domains() {
        return domains.findAll().stream().map(domain -> new DomainResponse(domain.getId(), domain.getName())).toList();
    }

    @GetMapping("/interview-types")
    public List<InterviewTypeResponse> interviewTypes() {
        return types.findAll().stream().map(type -> new InterviewTypeResponse(
                type.getId(), type.getName(), type.getDescription(), type.getSections().stream()
                        .map(section -> new SectionResponse(section.getId(), section.getName(), section.getOrderIndex(), section.getDurationMinutes()))
                        .toList())).toList();
    }

    public record DomainResponse(UUID id, String name) {}
    public record InterviewTypeResponse(UUID id, String name, String description, List<SectionResponse> sections) {}
    public record SectionResponse(UUID id, String name, int orderIndex, int durationMinutes) {}
}