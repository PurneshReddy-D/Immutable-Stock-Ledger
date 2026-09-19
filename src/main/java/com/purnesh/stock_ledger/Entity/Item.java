package com.purnesh.stock_ledger.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name="items")
@NoArgsConstructor
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    private boolean active = true;

    public Item(String code, String name, String unit) {
        this.code = code;
        this.name = name;
        this.unit = unit;
        this.active = true;
    }

    public void disable() {
        this.active = false;
    }


    public void rename(String newName) {
        this.name = newName;
    }
}
