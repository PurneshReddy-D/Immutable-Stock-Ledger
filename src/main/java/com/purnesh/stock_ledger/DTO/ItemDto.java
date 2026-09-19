package com.purnesh.stock_ledger.DTO;

import jakarta.validation.constraints.NotBlank;

public class ItemDto {

    public static class CreateItemRequest {
        @NotBlank
        public String code;
        @NotBlank
        public String name;
        @NotBlank
        public String unit;
    }

    public record ItemResponse(Long id, String code, String name, String unit, boolean active) {
    }
}
