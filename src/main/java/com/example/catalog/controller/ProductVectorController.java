package com.example.catalog.controller;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.service.ProductVectorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/products")
public class ProductVectorController {

    private final ProductVectorService productVectorService;

    public ProductVectorController(ProductVectorService productVectorService) {
        this.productVectorService = productVectorService;
    }

    @GetMapping("/vector-search")
    public List<ProductDocument> vectorSearch(
            @RequestParam String query) {

        return productVectorService.vectorSearch(query);
    }

}
