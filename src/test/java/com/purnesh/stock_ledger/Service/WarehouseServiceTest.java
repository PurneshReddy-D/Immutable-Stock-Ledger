package com.purnesh.stock_ledger.Service;

import com.purnesh.stock_ledger.AbstractIntegrationTests;
import com.purnesh.stock_ledger.Entity.Warehouse;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class WarehouseServiceTest extends AbstractIntegrationTests {
    @Autowired
    private WarehouseService warehouseService;

    private String uniqueCode() {
        return "WH-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void createAndFetchByCode() {
        String code = uniqueCode();
        warehouseService.create(code, "Main Warehouse");

        Warehouse found = warehouseService.getByCode(code);
        assertThat(found.getName()).isEqualTo("Main Warehouse");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void duplicateCode_isRejected() {
        String code = uniqueCode();
        warehouseService.create(code, "Main Warehouse");

        assertThatThrownBy(() -> warehouseService.create(code, "Another Warehouse"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void disable_setsActiveFalse() {
        String code = uniqueCode();
        warehouseService.create(code, "Main Warehouse");

        warehouseService.disable(code);

        assertThat(warehouseService.getByCode(code).isActive()).isFalse();
    }
}
