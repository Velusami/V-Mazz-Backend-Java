package com.example.catalog.controller;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.repository.ProductSearchRepository;
import com.example.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/search")
@Tag(name = "Product Search", description = "Elasticsearch Product Search APIs")
public class ProductSearchController {
    private final ProductSearchRepository repository;

    @Autowired
    ProductService productService;

    public ProductSearchController(ProductSearchRepository repository) {
        this.repository = repository;
    }

//    @PostMapping
//    public ProductDocument save() {
//
//        ProductDocument product = ProductDocument.builder()
//                .id(1001L)
//                .mpn("ABC-123")
//                .itemName("10A Miniature Circuit Breaker")
//                .manufacturer("Schneider")
//                .price(new BigDecimal("15.50"))
//                .currency("USD")
//                .quantity(250)
//                .status("ACTIVE")
//                .version(1L)
//                .build();
//
//        return repository.save(product);
//    }

    @GetMapping
    @Operation(summary = "Search products using Elasticsearch")
    public ResponseEntity<List<ProductDocument>> search(
            @RequestParam String keyword) {

        return ResponseEntity.ok(productService.search(keyword));
    }
}
