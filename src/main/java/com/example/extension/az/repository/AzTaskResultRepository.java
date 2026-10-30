package com.example.extension.az.repository;

import com.example.extension.az.model.entity.AzTaskResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AzTaskResultRepository extends JpaRepository<AzTaskResult, Long> {
	List<AzTaskResult> findByOrderIdOrderById(UUID orderId);
}
