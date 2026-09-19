package com.purnesh.stock_ledger.Service;

import com.purnesh.stock_ledger.Entity.Item;
import com.purnesh.stock_ledger.Execptions.BusinessRuleException;
import com.purnesh.stock_ledger.Execptions.NotFoundException;
import com.purnesh.stock_ledger.Repository.ItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public Item create(String code, String name, String unit) {
        if (itemRepository.existsByCode(code)) {
            throw new BusinessRuleException("An item with code '" + code + "' already exists");
        }
        return itemRepository.save(new Item(code, name, unit));
    }

    public List<Item> list() {
        return itemRepository.findAll();
    }

    public Item getByCode(String code) {
        return itemRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Item not found: " + code));
    }

    @Transactional
    public Item disable(String code) {
        Item item = getByCode(code);
        item.disable();
        return item;
    }


    @Transactional
    public Item rename(String code, String newName) {
        Item item = getByCode(code);
        item.rename(newName);
        return item;
    }
}
