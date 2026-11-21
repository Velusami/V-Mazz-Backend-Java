package com.example.catalog.vmazz.controller;

import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.dto.ItemRequest;
import com.example.catalog.vmazz.dto.ItemResponse;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.dto.StockUpdateRequest;
import com.example.catalog.vmazz.service.ItemService;
import com.example.catalog.vmazz.service.UploadStorageService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/items")
@RequiredArgsConstructor
@Tag(name = "3. Item Master (admin / sub user)", description = "Maintain the company's items")
public class AdminItemController {

    private final ItemService itemService;
    private final UploadStorageService uploadStorageService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an item image and return its persistent URL")
    public java.util.Map<String, String> uploadImage(@org.springframework.web.bind.annotation.RequestPart("file") MultipartFile file) {
        return java.util.Map.of("imageUrl", uploadStorageService.saveImage(file, "items"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an item (item code is generated when blank)")
    public ItemResponse create(@Valid @RequestBody ItemRequest request) {
        return itemService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an item (the item code does not change)")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return itemService.update(id, request);
    }

    @PutMapping("/{id}/stock")
    @Operation(summary = "Set the available quantity")
    public ItemResponse updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        return itemService.updateStock(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the company's items")
    public ItemResponse get(@PathVariable Long id) {
        return itemService.get(id);
    }

    @GetMapping
    @Operation(summary = "List the company's items (any status)")
    public PageResponse<ItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return itemService.list(q, category, status,
                Paging.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an item (admin only) - it is hidden from the storefront but kept for past orders")
    public ItemResponse deactivate(@PathVariable Long id) {
        return itemService.deactivate(id);
    }
}
