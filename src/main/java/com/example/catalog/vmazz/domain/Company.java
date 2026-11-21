package com.example.catalog.vmazz.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A supplier company on the portal. Its admin and sub users manage the company's item master. */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String companyCode;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(length = 20)
    private String gstNumber;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RecordStatus.ACTIVE;
        }
    }
}
