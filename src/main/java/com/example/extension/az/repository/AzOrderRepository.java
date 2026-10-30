package com.example.extension.az.repository;

import com.example.extension.az.model.entity.AzOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AzOrderRepository extends JpaRepository<AzOrder, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select customerOrder from AzOrder customerOrder where customerOrder.id = :orderId")
	Optional<AzOrder> findForUpdate(@Param("orderId") UUID orderId);
}
