package com.peerview.catalog;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewTypeRepository extends JpaRepository<InterviewType, UUID> {}