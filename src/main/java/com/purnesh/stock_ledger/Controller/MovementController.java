package com.purnesh.stock_ledger.Controller;
import com.purnesh.stock_ledger.DTO.MovementDto.MovementResponse;
import com.purnesh.stock_ledger.DTO.MovementDto.StockResponse;
import com.purnesh.stock_ledger.DTO.MovementDto.RecordMovementRequest;
import com.purnesh.stock_ledger.DTO.PageResponse;
import com.purnesh.stock_ledger.Entity.Movement;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import com.purnesh.stock_ledger.Service.MovementService;
import com.purnesh.stock_ledger.Service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class MovementController {
    private final MovementService movementService;
    private final WarehouseService warehouseService;

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse record(@Valid @RequestBody RecordMovementRequest req) {
        Movement m = movementService.record(req);
        return toResponse(m);
    }

    @PostMapping("/movements/{id}/cancel")
    public MovementResponse cancel(@PathVariable Long id, @RequestBody(required = false) CancelRequest body) {
        String recordedBy = (body != null && body.recordedBy != null) ? body.recordedBy : "unknown";
        String reason = (body != null) ? body.reason : null;
        Movement reversal = movementService.cancel(id, recordedBy, reason);
        return toResponse(reversal);
    }

    @GetMapping("/movements")
    public PageResponse<MovementResponse> history(
            @RequestParam String itemCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 100) {
            throw new BusinessRuleException("size must not exceed 100");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Movement> result = movementService.history(itemCode, pageable);
        return PageResponse.of(result.map(this::toResponse));
    }

    @GetMapping("/stock/current")
    public StockResponse currentStock(
            @RequestParam String itemCode,
            @RequestParam(required = false) String warehouseCode) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal qty = (warehouseCode != null)
                ? movementService.currentStockInWarehouse(itemCode, warehouseCode)
                : movementService.currentStockAcrossAllWarehouses(itemCode);
        return new StockResponse(itemCode, warehouseCode, qty, now);
    }

    @GetMapping("/stock/at-date")
    public StockResponse stockAtDate(
            @RequestParam String itemCode,
            @RequestParam(required = false) String warehouseCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        BigDecimal qty = (warehouseCode != null)
                ? movementService.stockInWarehouseAsOf(itemCode, warehouseCode, at)
                : movementService.stockAcrossAllWarehousesAsOf(itemCode, at);
        return new StockResponse(itemCode, warehouseCode, qty, at);
    }

    private MovementResponse toResponse(Movement m) {
        return new MovementResponse(
                m.getId(),
                m.getKind(),
                m.getItemCodeSnapshot(),
                m.getItemNameSnapshot(),
                m.getItemUnitSnapshot(),
                m.getQuantity(),
                warehouseService.codeForId(m.getWarehouseId()),
                warehouseService.codeForId(m.getFromWarehouseId()),
                warehouseService.codeForId(m.getToWarehouseId()),
                m.getReason(),
                m.getOccurredAt(),
                m.getRecordedAt(),
                m.getRecordedBy(),
                m.getCancelsMovementId(),
                movementService.isCancelled(m.getId())
        );
    }

    public static class CancelRequest {
        public String recordedBy;
        public String reason;
    }
}
