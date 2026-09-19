package com.purnesh.stock_ledger.Repository;

import com.purnesh.stock_ledger.Entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse,Long> {
    Optional<Warehouse> findByCode(String code);
    boolean existsByCode(String code);
}
