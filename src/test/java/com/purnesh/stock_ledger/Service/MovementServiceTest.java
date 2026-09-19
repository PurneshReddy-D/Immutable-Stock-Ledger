package com.purnesh.stock_ledger.Service;


import com.purnesh.stock_ledger.AbstractIntegrationTests;
import com.purnesh.stock_ledger.Entity.Item;

import com.purnesh.stock_ledger.Entity.Movement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.purnesh.stock_ledger.DTO.MovementDto.RecordMovementRequest;
import com.purnesh.stock_ledger.Entity.MovementKind;
import com.purnesh.stock_ledger.Entity.Warehouse;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.purnesh.stock_ledger.Entity.MovementKind.IN;
import static com.purnesh.stock_ledger.Entity.MovementKind.OUT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class MovementServiceTest extends AbstractIntegrationTests {
    @Autowired
    private MovementService movementService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private WarehouseService warehouseService;

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private RecordMovementRequest request(MovementKind kind, String itemCode,
                                                      String warehouseCode, String fromWh, String toWh,
                                                      BigDecimal qty, LocalDateTime occurredAt) {
        RecordMovementRequest req = new RecordMovementRequest();
        req.kind = kind;
        req.itemCode = itemCode;
        req.warehouseCode = warehouseCode;
        req.fromWarehouseCode = fromWh;
        req.toWarehouseCode = toWh;
        req.quantity = qty;
        req.reason = "test";
        req.occurredAt = occurredAt;
        req.recordedBy = "tester";
        return req;
    }

    @Test
    void inMovement_increasesCurrentStock() {
        Item item = itemService.create(uniqueCode("PEN"), "Blue Pen", "pieces");
        Warehouse wh = warehouseService.create(uniqueCode("WH"), "Main Warehouse");

        movementService.record(request(IN, item.getCode(), wh.getCode(), null, null,
                new BigDecimal("50"), LocalDateTime.now().minusDays(1)));

        BigDecimal stock = movementService.currentStockInWarehouse(item.getCode(), wh.getCode());
        assertThat(stock).isEqualByComparingTo("50");
    }

    @Test
    void outMovement_isRefused_whenInsufficientStock() {
        Item item = itemService.create(uniqueCode("PEN"), "Blue Pen", "pieces");
        Warehouse wh = warehouseService.create(uniqueCode("WH"), "Main Warehouse");

        // no stock in yet
        assertThatThrownBy(() ->
                movementService.record(request(OUT, item.getCode(), wh.getCode(), null, null,
                        new BigDecimal("10"), LocalDateTime.now())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void transfer_movesStockBetweenWarehouses_totalUnchanged() {
        Item item = itemService.create(uniqueCode("PEN"), "Blue Pen", "pieces");
        Warehouse whA = warehouseService.create(uniqueCode("WHA"), "Warehouse A");
        Warehouse whB = warehouseService.create(uniqueCode("WHB"), "Warehouse B");

        LocalDateTime t0 = LocalDateTime.now().minusDays(2);
        movementService.record(request(IN, item.getCode(), whA.getCode(), null, null,
                new BigDecimal("100"), t0));

        movementService.record(request(MovementKind.TRANSFER, item.getCode(), null,
                whA.getCode(), whB.getCode(), new BigDecimal("30"), t0.plusHours(1)));

        assertThat(movementService.currentStockInWarehouse(item.getCode(), whA.getCode()))
                .isEqualByComparingTo("70");
        assertThat(movementService.currentStockInWarehouse(item.getCode(), whB.getCode()))
                .isEqualByComparingTo("30");
        assertThat(movementService.currentStockAcrossAllWarehouses(item.getCode()))
                .isEqualByComparingTo("100"); // total unaffected by internal transfer
    }

    @Test
    void stockAtPastDate_ignoresMovementsAfterThatDate() {
        Item item = itemService.create(uniqueCode("PEN"), "Blue Pen", "pieces");
        Warehouse wh = warehouseService.create(uniqueCode("WH"), "Main Warehouse");

        LocalDateTime day1 = LocalDateTime.now().minusDays(10);
        LocalDateTime day2 = LocalDateTime.now().minusDays(5);
        LocalDateTime day3 = LocalDateTime.now().minusDays(1);

        movementService.record(request(IN, item.getCode(), wh.getCode(), null, null,
                new BigDecimal("20"), day1));
        movementService.record(request(IN, item.getCode(), wh.getCode(), null, null,
                new BigDecimal("30"), day2));
        movementService.record(request(IN, item.getCode(), wh.getCode(), null, null,
                new BigDecimal("15"), day3));


        BigDecimal stockAtDay2 = movementService.stockInWarehouseAsOf(
                item.getCode(), wh.getCode(), day2.plusHours(1));
        assertThat(stockAtDay2).isEqualByComparingTo("50");

        BigDecimal stockNow = movementService.currentStockInWarehouse(item.getCode(), wh.getCode());
        assertThat(stockNow).isEqualByComparingTo("65");
    }

    @Test
    void cancelMovement_reversesEffect_andCannotBeCancelledTwice() {
        Item item = itemService.create(uniqueCode("PEN"), "Blue Pen", "pieces");
        Warehouse wh = warehouseService.create(uniqueCode("WH"), "Main Warehouse");

        Movement original = movementService.record(request(IN, item.getCode(), wh.getCode(),
                null, null, new BigDecimal("40"), LocalDateTime.now().minusDays(1)));

        assertThat(movementService.currentStockInWarehouse(item.getCode(), wh.getCode()))
                .isEqualByComparingTo("40");

        Movement reversal = movementService.cancel(original.getId(), "tester", "mistake");


        assertThat(reversal.getCancelsMovementId()).isEqualTo(original.getId());
        assertThat(reversal.getKind()).isEqualTo(OUT);

        assertThat(movementService.currentStockInWarehouse(item.getCode(), wh.getCode()))
                .isEqualByComparingTo("0");

        assertThat(movementService.isCancelled(original.getId())).isTrue();


        assertThatThrownBy(() -> movementService.cancel(original.getId(), "tester", "again"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been cancelled");
    }

    @Test
    void renamingItem_doesNotChangeOldMovementSnapshot() {
        Item item = itemService.create(uniqueCode("PEN"), "Blue Pen", "pieces");
        Warehouse wh = warehouseService.create(uniqueCode("WH"), "Main Warehouse");

        Movement oldMovement = movementService.record(request(IN, item.getCode(), wh.getCode(),
                null, null, new BigDecimal("10"), LocalDateTime.now().minusDays(30)));

        assertThat(oldMovement.getItemNameSnapshot()).isEqualTo("Blue Pen");


        itemService.rename(item.getCode(), "Blue Gel Pen");


        Movement reread = movementService.history(item.getCode(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent().stream()
                .filter(m -> m.getId().equals(oldMovement.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(reread.getItemNameSnapshot()).isEqualTo("Blue Pen");
    }

    @Test
    void disabledItem_refusesNewMovements() {
        Item item = itemService.create(uniqueCode("PEN"), "Blue Pen", "pieces");
        Warehouse wh = warehouseService.create(uniqueCode("WH"), "Main Warehouse");
        itemService.disable(item.getCode());

        assertThatThrownBy(() ->
                movementService.record(request(IN, item.getCode(), wh.getCode(), null, null,
                        new BigDecimal("5"), LocalDateTime.now())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("disabled");
    }
}
