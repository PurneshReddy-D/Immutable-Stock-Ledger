package com.purnesh.stock_ledger.Service;


import com.purnesh.stock_ledger.DTO.MovementDto.RecordMovementRequest;
import com.purnesh.stock_ledger.Entity.Item;
import com.purnesh.stock_ledger.Entity.Movement;
import com.purnesh.stock_ledger.Entity.MovementKind;
import com.purnesh.stock_ledger.Entity.Warehouse;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import com.purnesh.stock_ledger.Execptions.NotFoundException;
import com.purnesh.stock_ledger.Repository.MovementRepository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.purnesh.stock_ledger.Entity.MovementKind.*;


@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final ItemService itemService;
    private final WarehouseService warehouseService;

    private static final boolean ALLOW_NEGATIVE_STOCK = false;

    public MovementService(MovementRepository movementRepository, ItemService itemService, WarehouseService warehouseService) {
        this.movementRepository = movementRepository;
        this.itemService = itemService;
        this.warehouseService = warehouseService;
    }

    @Transactional
    public Movement record(RecordMovementRequest req) {
        Item item = itemService.getByCode(req.itemCode);
        if (!item.isActive()) {
            throw new BusinessRuleException("Item '" + req.itemCode + "' is disabled; new movements are refused");
        }
        if (req.quantity == null || req.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("quantity must be greater than zero");
        }

        Movement.Builder builder = Movement.builder()
                .kind(req.kind)
                .itemId(item.getId())
                .itemCodeSnapshot(item.getCode())
                .itemNameSnapshot(item.getName())
                .itemUnitSnapshot(item.getUnit())
                .quantity(req.quantity)
                .reason(req.reason)
                .occurredAt(req.occurredAt)
                .recordedBy(req.recordedBy);

        switch (req.kind) {
            case IN -> {
                Warehouse wh = requireActiveWarehouse(req.warehouseCode, "warehouseCode is required for IN");
                builder.warehouseId(wh.getId());
            }
            case OUT -> {
                Warehouse wh = requireActiveWarehouse(req.warehouseCode, "warehouseCode is required for OUT");
                assertSufficientStock(item.getId(), wh.getId(), req.quantity, req.occurredAt);
                builder.warehouseId(wh.getId());
            }
            case TRANSFER -> {
                if (req.fromWarehouseCode == null || req.toWarehouseCode == null) {
                    throw new BusinessRuleException("fromWarehouseCode and toWarehouseCode are required for TRANSFER");
                }
                if (req.fromWarehouseCode.equals(req.toWarehouseCode)) {
                    throw new BusinessRuleException("fromWarehouseCode and toWarehouseCode must be different");
                }
                Warehouse from = requireActiveWarehouse(req.fromWarehouseCode, "fromWarehouseCode is required for TRANSFER");
                Warehouse to = requireActiveWarehouse(req.toWarehouseCode, "toWarehouseCode is required for TRANSFER");
                assertSufficientStock(item.getId(), from.getId(), req.quantity, req.occurredAt);
                builder.fromWarehouseId(from.getId()).toWarehouseId(to.getId());
            }
        }

        return movementRepository.save(builder.build());
    }


    @Transactional
    public Movement cancel(Long movementId, String recordedBy, String reason) {
        Movement original = movementRepository.findById(movementId)
                .orElseThrow(() -> new NotFoundException("Movement not found: " + movementId));

        if (movementRepository.findByCancelsMovementId(movementId).isPresent()) {
            throw new BusinessRuleException("Movement " + movementId + " has already been cancelled");
        }

        if (original.getCancelsMovementId() != null) {
            throw new BusinessRuleException("Movement " + movementId + " is itself a cancellation and cannot be cancelled");
        }

        LocalDateTime now = LocalDateTime.now();

        Movement.Builder reversal = Movement.builder()
                .itemId(original.getItemId())
                .itemCodeSnapshot(original.getItemCodeSnapshot())
                .itemNameSnapshot(original.getItemNameSnapshot())
                .itemUnitSnapshot(original.getItemUnitSnapshot())
                .quantity(original.getQuantity())
                .reason(reason != null && !reason.isBlank() ? reason : "Cancellation of movement " + movementId)
                .occurredAt(now)
                .recordedBy(recordedBy)
                .cancelsMovementId(movementId);

        switch (original.getKind()) {
            case IN -> {
                reversal.kind(OUT).warehouseId(original.getWarehouseId());
            }
            case OUT -> {
                reversal.kind(IN).warehouseId(original.getWarehouseId());
            }
            case TRANSFER -> {
                // Reverse direction: swap from/to
                reversal.kind(TRANSFER)
                        .fromWarehouseId(original.getToWarehouseId())
                        .toWarehouseId(original.getFromWarehouseId());
            }
        }

        return movementRepository.save(reversal.build());
    }

    @Transactional(readOnly = true)
    public BigDecimal currentStockInWarehouse(String itemCode, String warehouseCode) {
        return stockInWarehouseAsOf(itemCode, warehouseCode, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal currentStockAcrossAllWarehouses(String itemCode) {
        return stockAcrossAllWarehousesAsOf(itemCode, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal stockInWarehouseAsOf(String itemCode, String warehouseCode, LocalDateTime asOf) {
        Item item = itemService.getByCode(itemCode);
        Warehouse warehouse = warehouseService.getByCode(warehouseCode);
        return movementRepository.stockForItemInWarehouse(item.getId(), warehouse.getId(), asOf);
    }

    @Transactional(readOnly = true)
    public BigDecimal stockAcrossAllWarehousesAsOf(String itemCode, LocalDateTime asOf) {
        Item item = itemService.getByCode(itemCode);
        return movementRepository.stockForItemAcrossAllWarehouses(item.getId(), asOf);
    }

    @Transactional(readOnly = true)
    public Page<Movement> history(String itemCode, Pageable pageable) {
        Item item = itemService.getByCode(itemCode);
        return movementRepository.findByItemIdOrderByRecordedAtDescIdDesc(item.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public boolean isCancelled(Long movementId) {
        return movementRepository.findByCancelsMovementId(movementId).isPresent();
    }

    private Warehouse requireActiveWarehouse(String code, String missingMessage) {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException(missingMessage);
        }
        Warehouse wh = warehouseService.getByCode(code);
        if (!wh.isActive()) {
            throw new BusinessRuleException("Warehouse '" + code + "' is disabled; new movements are refused");
        }
        return wh;
    }

    private void assertSufficientStock(Long itemId, Long warehouseId, BigDecimal requestedOut, LocalDateTime occurredAt) {
        if (ALLOW_NEGATIVE_STOCK) {
            return;
        }
        BigDecimal current = movementRepository.stockForItemInWarehouse(itemId, warehouseId, occurredAt);
        if (current.compareTo(requestedOut) < 0) {
            throw new BusinessRuleException(
                    "Insufficient stock: requested " + requestedOut + " but only " + current + " available");
        }
    }


}
