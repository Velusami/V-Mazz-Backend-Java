package com.example.catalog.controller;

import com.example.catalog.dto.AlternatesResult;
import com.example.catalog.service.AlternateProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/products")
@RequiredArgsConstructor
@Tag(name = "Alternate Parts", description = "Alternate MPN suggestions")
public class AlternateProductController {

    private final AlternateProductService alternateProductService;

    // MPN is a query parameter (not a path variable) because MPNs can contain '/' and '.'
    @GetMapping("/alternates")
    @Operation(summary = "Suggest alternate parts for an MPN")
    public AlternatesResult alternates(
            @RequestParam String mpn,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "true") boolean inStockOnly,
            @RequestParam(defaultValue = "false") boolean differentManufacturer,
            @RequestParam(defaultValue = "true") boolean sameCategory,
            @RequestParam(defaultValue = "false") boolean samePackage) {

        return alternateProductService.findAlternates(
                mpn, limit, inStockOnly, differentManufacturer, sameCategory, samePackage);
    }
}
