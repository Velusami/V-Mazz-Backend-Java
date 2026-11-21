package com.example.catalog.vmazz.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Item master record: one sellable item of a supplier company. */
@Entity
@Table(name = "items", indexes = {
        @Index(name = "idx_item_category", columnList = "category"),
        @Index(name = "idx_item_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemCategory category;

    @Column(length = 100)
    private String subCategory;

    @Column(length = 100)
    private String brand;

    @Column(length = 1000)
    private String description;

    /** Item specification (grade, size, composition, shelf life ...). */
    @Column(length = 2000)
    private String specification;

    @Column(length = 20)
    private String hsnCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Uom uom;

    /** Selling price per unit of measure. */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    /** Maximum retail price (optional, used to show a discount). */
    @Column(precision = 18, scale = 2)
    private BigDecimal mrp;

    @Column(nullable = false, length = 3)
    private String currency;

    /** GST / tax percentage. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercent;

    /** Stock on hand, in the item's unit of measure. */
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal availableQuantity;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal minOrderQuantity;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 150)
    private String createdBy;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = RecordStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
