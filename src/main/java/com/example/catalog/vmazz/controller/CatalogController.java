package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.Uom;
import com.example.catalog.vmazz.dto.CategoryResponse;
import com.example.catalog.vmazz.dto.ItemResponse;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "2. Storefront (public)", description = "Browse and search items - no login needed")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/items")
    @Operation(summary = "Search items",
            description = "Filters: q (name, code, brand, sub category), category, brand, minPrice, maxPrice, "
                    + "inStockOnly, supplierId. sort = newest | price_asc | price_desc | name")
    public PageResponse<ItemResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return catalogService.search(q, category, brand, minPrice, maxPrice, inStockOnly, supplierId, sort, page, size);
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "Item details")
    public ItemResponse get(@PathVariable Long id) {
        return catalogService.get(id);
    }

    @GetMapping("/categories")
    @Operation(summary = "Categories with the number of active items")
    public List<CategoryResponse> categories() {
        return catalogService.categories();
    }

    @GetMapping("/uoms")
    @Operation(summary = "Units of measure")
    public List<Uom> uoms() {
        return catalogService.uoms();
    }
}
