package com.example.catalog.repository;

import com.example.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByMpn(String mpn);
    Optional<Product> findByMpnIgnoreCase(String mpn);
    boolean existsByMpn(String mpn);
}
