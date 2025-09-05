package com.creditx.promo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.creditx.promo.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>{
    
}
