package com.purnesh.stock_ledger.Controller;

import com.purnesh.stock_ledger.DTO.WarehouseDto.CreateWarehouseRequest;
import com.purnesh.stock_ledger.Entity.Warehouse;
import com.purnesh.stock_ledger.Service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.purnesh.stock_ledger.DTO.WarehouseDto.WarehouseResponse;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody CreateWarehouseRequest req) {
        Warehouse wh = warehouseService.create(req.code, req.name);
        return toResponse(wh);
    }

    @GetMapping
    public List<WarehouseResponse> list() {
        return warehouseService.list().stream().map(WarehouseController::toResponse).toList();
    }

    @PatchMapping("/{code}/disable")
    public WarehouseResponse disable(@PathVariable String code) {
        return toResponse(warehouseService.disable(code));
    }

    private static WarehouseResponse toResponse(Warehouse wh) {
        return new WarehouseResponse(wh.getId(), wh.getCode(), wh.getName(), wh.isActive());
    }
}