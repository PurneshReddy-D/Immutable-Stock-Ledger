package com.purnesh.stock_ledger.Controller;

import com.purnesh.stock_ledger.DTO.ItemDto.CreateItemRequest;
import com.purnesh.stock_ledger.DTO.ItemDto.ItemResponse;
import com.purnesh.stock_ledger.Entity.Item;
import com.purnesh.stock_ledger.Service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody CreateItemRequest req) {
        Item item = itemService.create(req.code, req.name, req.unit);
        return toResponse(item);
    }

    @GetMapping("/get-items")
    public List<ItemResponse> list() {
        return itemService.list().stream().map(ItemController::toResponse).toList();
    }

    @PatchMapping("/{code}/disable")
    public ItemResponse disable(@PathVariable String code) {
        return toResponse(itemService.disable(code));
    }


    @PatchMapping("/{code}/rename")
    public ItemResponse rename(@PathVariable String code, @RequestBody RenameRequest req) {
        return toResponse(itemService.rename(code, req.name));
    }

    public static class RenameRequest {
        public String name;
    }

    private static ItemResponse toResponse(Item item) {
        return new ItemResponse(item.getId(), item.getCode(), item.getName(), item.getUnit(), item.isActive());
    }
}
