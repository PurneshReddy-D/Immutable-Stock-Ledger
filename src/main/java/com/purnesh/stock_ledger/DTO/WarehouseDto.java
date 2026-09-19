package com.purnesh.stock_ledger.DTO;

import jakarta.validation.constraints.NotBlank;

public class WarehouseDto {
    public static class CreateWarehouseRequest {
        @NotBlank
        public String code;
        @NotBlank
        public String name;
    }

    public record WarehouseResponse(Long id, String code, String name, boolean active) {
    }
}
