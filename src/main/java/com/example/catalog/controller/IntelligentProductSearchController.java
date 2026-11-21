
package com.example.catalog.controller;

import com.example.catalog.document.ProductDocument;
import com.example.catalog.service.IntelligentProductSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/products")
public class IntelligentProductSearchController {

    private final IntelligentProductSearchService searchService;

    public IntelligentProductSearchController(
            IntelligentProductSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/intelligent-search")
    public List<ProductDocument> intelligentSearch(
            @RequestParam("query") String query) {

        return searchService.search(query);
    }
}
