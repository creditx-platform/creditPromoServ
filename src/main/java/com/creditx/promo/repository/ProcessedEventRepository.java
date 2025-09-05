package com.creditx.promo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.creditx.promo.model.ProcessedEvent;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    
    boolean existsByEventId(String eventId);
    
    boolean existsByPayloadHash(String payloadHash);
}
