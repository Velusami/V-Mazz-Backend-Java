package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.AppUser;
import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import com.example.catalog.vmazz.dto.ItemRequest;
import com.example.catalog.vmazz.dto.ItemResponse;
import com.example.catalog.vmazz.dto.PageResponse;
import com.example.catalog.vmazz.dto.StockUpdateRequest;
import com.example.catalog.vmazz.exception.ConflictException;
import com.example.catalog.vmazz.exception.ResourceNotFoundException;
import com.example.catalog.vmazz.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

/** Item master maintained by company admins and sub users. Every call is limited to the caller's company. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private static final String CODE_PATTERN = "^[A-Z0-9][A-Z0-9._/-]{1,49}$";

    private final ItemRepository itemRepository;
    private final CurrentUserService currentUser;

    @Transactional
    public ItemResponse create(ItemRequest request) {

        AppUser user = currentUser.requireCompanyUser();
        validate(request);

        String code = normaliseCode(request.itemCode());
        if (code != null && itemRepository.existsByItemCodeIgnoreCase(code)) {
            throw new ConflictException("Item code already exists: " + code);
        }

        Item item = Item.builder()
                .company(user.getCompany())
                .itemCode(code != null ? code : "TMP-" + UUID.randomUUID())   // generated below when blank
                .createdBy(user.getEmail())
                .status(RecordStatus.ACTIVE)
                .build();
        apply(item, request);

        Item saved = itemRepository.save(item);
        if (code == null) {
            saved.setItemCode(String.format("ITM%06d", saved.getId()));
        }

        log.info("Item {} created by {}", saved.getItemCode(), user.getEmail());
        return ItemResponse.from(saved);
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest request) {

        AppUser user = currentUser.requireCompanyUser();
        validate(request);

        Item item = findOwned(id, user);
        apply(item, request);               // the item code never changes
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse updateStock(Long id, StockUpdateRequest request) {

        AppUser user = currentUser.requireCompanyUser();
        Item item = findOwned(id, user);

        QuantityRules.requireWholeIfCountable(item.getUom(), request.availableQuantity(), "Available quantity");
        item.setAvailableQuantity(request.availableQuantity().setScale(3, RoundingMode.HALF_UP));
        return ItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        AppUser user = currentUser.requireCompanyUser();
        return ItemResponse.from(findOwned(id, user));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> list(String q, ItemCategory category, RecordStatus status, Pageable pageable) {
        AppUser user = currentUser.requireCompanyUser();
        return PageResponse.of(
                itemRepository.findAll(
                        ItemSpecifications.forCompany(user.getCompany().getId(), q, category, status), pageable),
                ItemResponse::from);
    }

    /**
     * "Delete" = deactivate. Items are referenced by carts and purchase orders,
     * so they are hidden from the catalog instead of being removed.
     */
    @Transactional
    public ItemResponse deactivate(Long id) {
        AppUser user = currentUser.requireAdmin();
        Item item = findOwned(id, user);
        item.setStatus(RecordStatus.INACTIVE);
        return ItemResponse.from(item);
    }

    // ------------------------------------------------------------------

    private Item findOwned(Long id, AppUser user) {
        return itemRepository.findByIdAndCompany_Id(id, user.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));
    }

    private void validate(ItemRequest r) {

        if (r.mrp() != null && r.mrp().compareTo(r.unitPrice()) < 0) {
            throw new IllegalArgumentException("MRP cannot be lower than the unit price");
        }
        QuantityRules.requireWholeIfCountable(r.uom(), r.availableQuantity(), "Available quantity");
        if (r.minOrderQuantity() != null) {
            QuantityRules.requireWholeIfCountable(r.uom(), r.minOrderQuantity(), "Minimum order quantity");
        }
    }

    private void apply(Item item, ItemRequest r) {

        item.setItemName(r.itemName().trim());
        item.setCategory(r.category());
        item.setSubCategory(blankToNull(r.subCategory()));
        item.setBrand(blankToNull(r.brand()));
        item.setDescription(blankToNull(r.description()));
        item.setSpecification(blankToNull(r.specification()));
        item.setHsnCode(blankToNull(r.hsnCode()));
        item.setUom(r.uom());
        item.setUnitPrice(r.unitPrice().setScale(2, RoundingMode.HALF_UP));
        item.setMrp(r.mrp() == null ? null : r.mrp().setScale(2, RoundingMode.HALF_UP));
        item.setCurrency(r.currency() == null || r.currency().isBlank()
                ? "INR"
                : r.currency().trim().toUpperCase(Locale.ROOT));
        item.setTaxPercent((r.taxPercent() == null ? BigDecimal.ZERO : r.taxPercent())
                .setScale(2, RoundingMode.HALF_UP));
        item.setAvailableQuantity(r.availableQuantity().setScale(3, RoundingMode.HALF_UP));
        item.setMinOrderQuantity((r.minOrderQuantity() == null ? BigDecimal.ONE : r.minOrderQuantity())
                .setScale(3, RoundingMode.HALF_UP));
        item.setImageUrl(blankToNull(r.imageUrl()));
        if (r.status() != null) {
            item.setStatus(r.status());
        }
    }

    private String normaliseCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalised = code.trim().toUpperCase(Locale.ROOT);
        if (!normalised.matches(CODE_PATTERN)) {
            throw new IllegalArgumentException(
                    "Item code must be 2-50 characters: letters, digits, '.', '_', '/' or '-'");
        }
        return normalised;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
