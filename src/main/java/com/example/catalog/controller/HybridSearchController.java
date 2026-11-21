
package com.example.catalog.controller;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.service.HybridSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/products")
public class HybridSearchController {

    private final HybridSearchService hybridSearchService;

    public HybridSearchController(
            HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping("/hybrid-search")
    public Map<String, Object> hybridSearch(
            @RequestBody Map<String, String> request) {

        String query = request.get("query");

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Query must not be empty");
        }

        List<ProductDocument> products =
                hybridSearchService.search(query);

        List<Map<String, Object>> results = products.stream()
                .map(product -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("mpn", product.getMpn());
                    item.put("itemName", product.getItemName());
                    item.put("manufacturer", product.getManufacturer());
                    item.put("price", product.getPrice());
                    item.put("currency", product.getCurrency());
                    item.put("quantity", product.getQuantity());
                    item.put("status", product.getStatus());
                    return item;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("count", results.size());
        response.put("results", results);

        return response;
    }
}
