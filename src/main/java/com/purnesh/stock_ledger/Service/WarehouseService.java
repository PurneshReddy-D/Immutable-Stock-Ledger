package com.purnesh.stock_ledger.Service;

import com.purnesh.stock_ledger.Entity.Warehouse;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import com.purnesh.stock_ledger.Execptions.NotFoundException;
import com.purnesh.stock_ledger.Repository.WarehouseRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public Warehouse create(String code, String name) {
        if (warehouseRepository.existsByCode(code)) {
            throw new BusinessRuleException("A warehouse with code '" + code + "' already exists");
        }
        return warehouseRepository.save(new Warehouse(code, name));
    }

    public List<Warehouse> list() {
        return warehouseRepository.findAll();
    }

    public Warehouse getByCode(String code) {
        return warehouseRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Warehouse not found: " + code));
    }

    public String codeForId(Long id) {
        if (id == null) {
            return null;
        }
        return warehouseRepository.findById(id)
                .map(Warehouse::getCode)
                .orElse(null);
    }

    @Transactional
    public Warehouse disable(String code) {
        Warehouse warehouse = getByCode(code);
        warehouse.disable();
        return warehouse;
    }
}
