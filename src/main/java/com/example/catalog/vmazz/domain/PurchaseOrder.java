package com.example.catalog.vmazz.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase order header. Checkout creates one PO per supplier company in the cart.
 * Address, contact and prices are stored as a snapshot so later edits never change a placed order.
 */
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String poNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "supplier_company_id", nullable = false)
    private Company supplierCompany;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    private LocalDate requiredDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** The buyer's own PO / reference number. */
    @Column(length = 50)
    private String buyerReference;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, length = 150)
    private String contactName;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressLine1", column = @Column(name = "ship_address_line1", length = 200)),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "ship_address_line2", length = 200)),
            @AttributeOverride(name = "city", column = @Column(name = "ship_city", length = 100)),
            @AttributeOverride(name = "state", column = @Column(name = "ship_state", length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code", length = 10)),
            @AttributeOverride(name = "country", column = @Column(name = "ship_country", length = 100))
    })
    private Address shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "addressLine1", column = @Column(name = "bill_address_line1", length = 200)),
            @AttributeOverride(name = "addressLine2", column = @Column(name = "bill_address_line2", length = 200)),
            @AttributeOverride(name = "city", column = @Column(name = "bill_city", length = 100)),
            @AttributeOverride(name = "state", column = @Column(name = "bill_state", length = 100)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "bill_postal_code", length = 10)),
            @AttributeOverride(name = "country", column = @Column(name = "bill_country", length = 100))
    })
    private Address billingAddress;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subTotal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal taxTotal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    @Builder.Default
    private List<PurchaseOrderItem> items = new ArrayList<>();

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
