package com.example.catalog.vmazz.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** One line of a purchase order, with the item details copied at the time of ordering. */
@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int lineNumber;

    // ---- snapshot of the item master at order time ----
    @Column(nullable = false, length = 50)
    private String itemCode;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemCategory category;

    @Column(length = 2000)
    private String specification;

    @Column(length = 20)
    private String hsnCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Uom uom;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineSubTotal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal lineTotal;
}
