package com.purnesh.stock_ledger.Repository;

import com.purnesh.stock_ledger.Entity.Movement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
@Repository
public interface MovementRepository extends JpaRepository<Movement,Long> {


    Page<Movement> findByItemIdOrderByRecordedAtDescIdDesc(Long itemId, Pageable pageable);
    Optional<Movement> findByCancelsMovementId(Long movementId);


    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN m.kind = com.purnesh.stock_ledger.Entity.MovementKind.IN
                     AND m.warehouseId = :warehouseId THEN m.quantity
                WHEN m.kind = com.purnesh.stock_ledger.Entity.MovementKind.OUT
                     AND m.warehouseId = :warehouseId THEN -m.quantity
                WHEN m.kind = com.purnesh.stock_ledger.Entity.MovementKind.TRANSFER
                     AND m.toWarehouseId = :warehouseId THEN m.quantity
                WHEN m.kind = com.purnesh.stock_ledger.Entity.MovementKind.TRANSFER
                     AND m.fromWarehouseId = :warehouseId THEN -m.quantity
                ELSE 0
            END
        ), 0)
        FROM Movement m
        WHERE m.itemId = :itemId
          AND m.occurredAt <= :asOf
        """)
    BigDecimal stockForItemInWarehouse(@Param("itemId") Long itemId,
                                       @Param("warehouseId") Long warehouseId,
                                       @Param("asOf") LocalDateTime asOf);

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN m.kind = com.purnesh.stock_ledger.Entity.MovementKind.IN THEN m.quantity
                WHEN m.kind = com.purnesh.stock_ledger.Entity.MovementKind.OUT THEN -m.quantity
                ELSE 0
            END
        ), 0)
        FROM Movement m
        WHERE m.itemId = :itemId
          AND m.occurredAt <= :asOf
        """)
    BigDecimal stockForItemAcrossAllWarehouses(@Param("itemId") Long itemId,
                                               @Param("asOf") LocalDateTime asOf);
}
