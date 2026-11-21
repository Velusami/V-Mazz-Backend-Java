package com.example.catalog.controller;

import com.example.catalog.service.ProductRagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/products")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:4200"})
public class ProductRagController {

    private final ProductRagService productRagService;

    public ProductRagController(ProductRagService productRagService) {
        this.productRagService = productRagService;
    }

    @PostMapping("/rag")
    public ResponseEntity<Map<String, String>> rag(
            @RequestBody Map<String, String> request) {

        String query = request.get("query");

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Query cannot be empty"
                    ));
        }

        String answer = productRagService.ask(query);

        return ResponseEntity.ok(
                Map.of(
                        "query", query,
                        "answer", answer
                )
        );
    }

}
