package com.example.catalog.controller;

import com.example.catalog.service.ProductReindexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Maintenance APIs (restrict to ADMIN once security is added)")
public class AdminReindexController {

    private final ProductReindexService reindexService;

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild the Elasticsearch index from SQL")
    public Map<String, Object> reindex(
            @RequestParam(defaultValue = "false") boolean purge) {

        int count = reindexService.reindexAll(purge);
        return Map.of("indexed", count, "purged", purge);
    }
}
