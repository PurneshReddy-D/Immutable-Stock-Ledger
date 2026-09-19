package com.purnesh.stock_ledger.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "warehouses")
@Getter
@NoArgsConstructor
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    public Warehouse(String code, String name) {
        this.code = code;
        this.name = name;
        this.active = true;
    }

    public void disable() {
        this.active = false;
    }
}
