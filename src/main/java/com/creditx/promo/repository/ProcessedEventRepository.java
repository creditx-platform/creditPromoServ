package com.creditx.promo.repository;

import com.creditx.promo.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

  boolean existsByEventId(String eventId);

  boolean existsByPayloadHash(String payloadHash);
}
