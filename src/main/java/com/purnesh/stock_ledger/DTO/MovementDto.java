package com.purnesh.stock_ledger.DTO;

import com.purnesh.stock_ledger.Entity.MovementKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovementDto {

    public static class RecordMovementRequest {
        @NotNull
        public MovementKind kind;

        @NotBlank
        public String itemCode;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "quantity must be greater than zero")
        public BigDecimal quantity;


        public String warehouseCode;


        public String fromWarehouseCode;
        public String toWarehouseCode;

        @NotBlank
        public String reason;

        @NotNull
        public LocalDateTime occurredAt;

        @NotBlank
        public String recordedBy;
    }

    public record MovementResponse(
            Long id,
            MovementKind kind,
            String itemCode,
            String itemName,
            String itemUnit,
            BigDecimal quantity,
            String warehouseCode,
            String fromWarehouseCode,
            String toWarehouseCode,
            String reason,
            LocalDateTime occurredAt,
            LocalDateTime recordedAt,
            String recordedBy,
            Long cancelsMovementId,
            boolean cancelled
    ) {
    }

    public record StockResponse(String itemCode, String warehouseCode, BigDecimal quantity, LocalDateTime asOf) {
    }
}
