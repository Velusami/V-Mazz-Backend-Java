package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.domain.Uom;
import com.example.catalog.vmazz.dto.CategoryResponse;
import com.example.catalog.vmazz.dto.ItemResponse;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
import com.example.catalog.vmazz.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Public storefront: browse and search active items. */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> search(
            String q,
            ItemCategory category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            Long supplierCompanyId,
            String sort,
            int page,
            int size) {

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sortFor(sort));

        return PageResponse.of(
                itemRepository.findAll(
                        ItemSpecifications.catalog(q, category, brand, minPrice, maxPrice, inStockOnly, supplierCompanyId),
                        pageable),
                ItemResponse::from);
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        Item item = itemRepository.findById(id)
                .filter(i -> i.getStatus() == RecordStatus.ACTIVE
                        && i.getCompany().getStatus() == RecordStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));
        return ItemResponse.from(item);
    }

    /** All categories with the number of active items in each (0 when empty). */
    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {

        Map<ItemCategory, Long> counts = new EnumMap<>(ItemCategory.class);
        for (Object[] row : itemRepository.countByCategory(RecordStatus.ACTIVE)) {
            counts.put((ItemCategory) row[0], (Long) row[1]);
        }

        return Arrays.stream(ItemCategory.values())
                .map(c -> new CategoryResponse(c, c.getDisplayName(), counts.getOrDefault(c, 0L)))
                .toList();
    }

    public List<Uom> uoms() {
        return Arrays.asList(Uom.values());
    }

    // price_asc | price_desc | name | newest (default)
    private Sort sortFor(String sort) {
        String key = sort == null ? "newest" : sort.trim().toLowerCase();
        return switch (key) {
            case "price_asc" -> Sort.by(Sort.Order.asc("unitPrice"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("unitPrice"), Sort.Order.asc("id"));
            case "name" -> Sort.by(Sort.Order.asc("itemName"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }
}
