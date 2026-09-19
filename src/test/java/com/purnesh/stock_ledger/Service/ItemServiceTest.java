package com.purnesh.stock_ledger.Service;

import com.purnesh.stock_ledger.AbstractIntegrationTests;
import com.purnesh.stock_ledger.Entity.Item;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import com.purnesh.stock_ledger.Execptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class ItemServiceTest extends AbstractIntegrationTests {
    @Autowired
    private ItemService itemService;

    private String uniqueCode() {
        return "ITM-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void createAndFetchByCode() {
        String code = uniqueCode();
        itemService.create(code, "Blue Pen", "pieces");

        Item found = itemService.getByCode(code);
        assertThat(found.getName()).isEqualTo("Blue Pen");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void duplicateCode_isRejected() {
        String code = uniqueCode();
        itemService.create(code, "Blue Pen", "pieces");

        assertThatThrownBy(() -> itemService.create(code, "Another Pen", "pieces"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unknownCode_throwsNotFound() {
        assertThatThrownBy(() -> itemService.getByCode("DOES-NOT-EXIST"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void disable_setsActiveFalse() {
        String code = uniqueCode();
        itemService.create(code, "Blue Pen", "pieces");

        itemService.disable(code);

        assertThat(itemService.getByCode(code).isActive()).isFalse();
    }

}
