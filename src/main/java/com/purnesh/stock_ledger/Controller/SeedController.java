package com.purnesh.stock_ledger.Controller;

import com.purnesh.stock_ledger.Entity.Item;
import com.purnesh.stock_ledger.Entity.Warehouse;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import com.purnesh.stock_ledger.Service.ItemService;
import com.purnesh.stock_ledger.Service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SeedController {
    private final ItemService itemService;
    private final WarehouseService warehouseService;

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        List<Item> items = createIfMissing(
                new String[][]{
                        {"PEN-BLUE", "Blue Pen", "pieces"},
                        {"NOTE-A4", "A4 Notebook", "pieces"},
                        {"OIL-SUN", "Sunflower Oil", "litres"}
                });

        List<Warehouse> warehouses = createIfMissingWarehouses(
                new String[][]{
                        {"WH-NORTH", "North Warehouse"},
                        {"WH-SOUTH", "South Warehouse"}
                });

        return Map.of(
                "items", items,
                "warehouses", warehouses,
                "note", "You can now POST /movements using these codes."
        );
    }

    private List<Item> createIfMissing(String[][] rows) {
        return List.of(rows).stream()
                .map(r -> {
                    try {
                        return itemService.create(r[0], r[1], r[2]);
                    } catch (BusinessRuleException alreadyExists) {
                        return itemService.getByCode(r[0]);
                    }
                })
                .toList();
    }

    private List<Warehouse> createIfMissingWarehouses(String[][] rows) {
        return List.of(rows).stream()
                .map(r -> {
                    try {
                        return warehouseService.create(r[0], r[1]);
                    } catch (BusinessRuleException alreadyExists) {
                        return warehouseService.getByCode(r[0]);
                    }
                })
                .toList();
    }
}
