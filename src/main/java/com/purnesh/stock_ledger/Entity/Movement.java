package com.purnesh.stock_ledger.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name = "movements")
@Getter
@NoArgsConstructor
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementKind kind;

    @Column(nullable = false)
    private Long itemId;

    @Column(nullable = false, length = 50)
    private String itemCodeSnapshot;

    @Column(nullable = false, length = 150)
    private String itemNameSnapshot;

    @Column(nullable = false, length = 20)
    private String itemUnitSnapshot;


    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;


    @Column
    private Long warehouseId;


    @Column
    private Long fromWarehouseId;

    @Column
    private Long toWarehouseId;

    @Column(nullable = false, length = 500)
    private String reason;


    @Column(nullable = false)
    private LocalDateTime occurredAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false, length = 150)
    private String recordedBy;

    @Column
    private Long cancelsMovementId;

    private Movement(Builder b) {
        this.kind = b.kind;
        this.itemId = b.itemId;
        this.itemCodeSnapshot = b.itemCodeSnapshot;
        this.itemNameSnapshot = b.itemNameSnapshot;
        this.itemUnitSnapshot = b.itemUnitSnapshot;
        this.quantity = b.quantity;
        this.warehouseId = b.warehouseId;
        this.fromWarehouseId = b.fromWarehouseId;
        this.toWarehouseId = b.toWarehouseId;
        this.reason = b.reason;
        this.occurredAt = b.occurredAt;
        this.recordedAt = LocalDateTime.now();
        this.recordedBy = b.recordedBy;
        this.cancelsMovementId = b.cancelsMovementId;
    }

    public static Builder builder() {
        return new Builder();
    }



    public static class Builder {
        private MovementKind kind;
        private Long itemId;
        private String itemCodeSnapshot;
        private String itemNameSnapshot;
        private String itemUnitSnapshot;
        private BigDecimal quantity;
        private Long warehouseId;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private String reason;
        private LocalDateTime occurredAt;
        private String recordedBy;
        private Long cancelsMovementId;

        public Builder kind(MovementKind kind) { this.kind = kind; return this; }
        public Builder itemId(Long itemId) { this.itemId = itemId; return this; }
        public Builder itemCodeSnapshot(String v) { this.itemCodeSnapshot = v; return this; }
        public Builder itemNameSnapshot(String v) { this.itemNameSnapshot = v; return this; }
        public Builder itemUnitSnapshot(String v) { this.itemUnitSnapshot = v; return this; }
        public Builder quantity(BigDecimal quantity) { this.quantity = quantity; return this; }
        public Builder warehouseId(Long warehouseId) { this.warehouseId = warehouseId; return this; }
        public Builder fromWarehouseId(Long v) { this.fromWarehouseId = v; return this; }
        public Builder toWarehouseId(Long v) { this.toWarehouseId = v; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder occurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder recordedBy(String recordedBy) { this.recordedBy = recordedBy; return this; }
        public Builder cancelsMovementId(Long id) { this.cancelsMovementId = id; return this; }

        public Movement build() {
            return new Movement(this);
        }
    }
}
