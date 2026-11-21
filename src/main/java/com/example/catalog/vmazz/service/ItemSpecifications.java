package com.example.catalog.vmazz.service;

import com.example.catalog.vmazz.domain.Item;
import com.example.catalog.vmazz.domain.ItemCategory;
import com.example.catalog.vmazz.domain.RecordStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dynamic filters for item searches. */
final class ItemSpecifications {

    private ItemSpecifications() { }

    /** What a buyer sees: active items of active companies. */
    static Specification<Item> catalog(
            String q,
            ItemCategory category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            Long supplierCompanyId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), RecordStatus.ACTIVE));
            predicates.add(cb.equal(root.get("company").get("status"), RecordStatus.ACTIVE));

            addTextSearch(predicates, root, cb, q);

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (hasText(brand)) {
                predicates.add(cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase(Locale.ROOT)));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("unitPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<BigDecimal>get("unitPrice"), maxPrice));
            }
            if (inStockOnly) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<BigDecimal>get("availableQuantity"), root.<BigDecimal>get("minOrderQuantity")));
            }
            if (supplierCompanyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), supplierCompanyId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** What a company user sees: all items (any status) of their own company. */
    static Specification<Item> forCompany(Long companyId, String q, ItemCategory category, RecordStatus status) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("company").get("id"), companyId));
            addTextSearch(predicates, root, cb, q);

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addTextSearch(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Item> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String q) {

        if (!hasText(q)) {
            return;
        }
        String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(cb.or(
                cb.like(cb.lower(root.get("itemName")), like),
                cb.like(cb.lower(root.get("itemCode")), like),
                cb.like(cb.lower(root.get("brand")), like),
                cb.like(cb.lower(root.get("subCategory")), like),
                cb.like(cb.lower(root.get("description")), like)));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
