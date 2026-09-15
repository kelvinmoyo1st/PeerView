package com.peerview.catalog;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainRepository extends JpaRepository<Domain, UUID> {}