package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_mpn", columnList = "mpn")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String mpn;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false, length = 150)
    private String manufacturer;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer quantity;

    // Technical data used for alternate-part matching (optional)
    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String packageType;

    @Column(length = 500)
    private String specifications;

    @Column(nullable = false, length = 30)
    private String status;

    @Version
    private Long version;
}
