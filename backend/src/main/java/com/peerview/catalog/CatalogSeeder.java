package com.peerview.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogSeeder {

    @Bean
    CommandLineRunner seedCatalog(DomainRepository domains, InterviewTypeRepository types) {
        return args -> {
            if (domains.count() == 0) {
                domains.save(new Domain("Backend Engineering"));
                domains.save(new Domain("Data Science"));
                domains.save(new Domain("Product Management"));
            }
            if (types.count() == 0) {
                InterviewType technicalScreen = new InterviewType(
                        "Technical Screen", "A practical screen covering technical depth, communication, and candidate questions.");
                technicalScreen.addSection("Intro", 0, 5);
                technicalScreen.addSection("Technical", 1, 25);
                technicalScreen.addSection("HR / Behavioral", 2, 10);
                technicalScreen.addSection("Candidate Questions", 3, 5);
                types.save(technicalScreen);

                InterviewType systemDesign = new InterviewType(
                        "System Design", "A structured design conversation with tradeoffs, scale, and communication in focus.");
                systemDesign.addSection("Context", 0, 5);
                systemDesign.addSection("System Design", 1, 35);
                systemDesign.addSection("Tradeoffs", 2, 10);
                systemDesign.addSection("Candidate Questions", 3, 5);
                types.save(systemDesign);

                InterviewType behavioral = new InterviewType(
                        "Behavioral", "A reflective conversation about collaboration, ownership, and decision-making.");
                behavioral.addSection("Warm-up", 0, 5);
                behavioral.addSection("Experience", 1, 25);
                behavioral.addSection("Reflection", 2, 10);
                behavioral.addSection("Candidate Questions", 3, 5);
                types.save(behavioral);
            }
        };
    }
}