package com.example.consumer.repository;

import com.example.consumer.model.entity.ConsumerOrder;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumerOrderRepository extends JpaRepository<ConsumerOrder, UUID> {}
